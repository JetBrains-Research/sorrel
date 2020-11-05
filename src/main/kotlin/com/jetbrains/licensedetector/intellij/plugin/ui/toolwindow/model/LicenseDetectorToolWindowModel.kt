package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model

import arrow.core.Either
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.createNestedDisposable
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.util.Alarm
import com.jetbrains.licensedetector.intellij.plugin.module.ProjectModule
import com.jetbrains.licensedetector.intellij.plugin.utils.getSimpleIdentifier
import com.jetbrains.licensedetector.intellij.plugin.utils.getVersion
import com.jetbrains.packagesearch.intellij.plugin.api.SearchClient
import com.jetbrains.packagesearch.intellij.plugin.api.ServerURLs
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rd.util.reactive.Signal
import java.util.concurrent.atomic.AtomicInteger

class LicenseDetectorToolWindowModel(val project: Project, val lifetime: Lifetime) {

    private val application = ApplicationManager.getApplication()
    private val parentDisposable = lifetime.createNestedDisposable()

    private val searchClient = SearchClient(ServerURLs.base)
    private val refreshContextAlarmInterval: Long = 10000 // ms
    private val refreshContextAlarm = Alarm(parentDisposable)
    private val operationsCounter = AtomicInteger(0)

    // Observables
    val isBusy = Property(false)
    val isFetchingSuggestions = Property(false)

    val searchTerm = Property("")
    val searchResults = Property(mapOf<String, LicenseDetectorDependency>())
    private val installedPackages = Property(mapOf<String, LicenseDetectorDependency>())

    val projectModules = Property(listOf<ProjectModule>())

    val selectedProjectModule = Property<ProjectModule?>(null)
    val selectedPackage = Property("")

    // UI Signals
    val requestRefreshContext = Signal<Boolean>()
    val searchResultsUpdated = Signal<Map<String, LicenseDetectorDependency>>()

    private fun startOperation() {
        isBusy.set(operationsCounter.incrementAndGet() > 0)
    }

    private fun finishOperation() {
        isBusy.set(operationsCounter.decrementAndGet() > 0)
    }


    // Implementation
    init {
        // Populate foundPackages when either:
        // - list of installed packages changes
        // - selected module changes
        installedPackages.advise(lifetime) {
            refreshFoundPackages()
        }
        selectedProjectModule.advise(lifetime) {
            refreshFoundPackages()
        }

        // Fetch installed packages and available project modules automatically, and when requested
        val delayMillis = 250
        refreshContextAlarm.addRequest(::autoRefreshContext, delayMillis)
        requestRefreshContext.advise(lifetime) {
            refreshContext()
        }
    }


    @Suppress("ComplexMethod")
    private fun refreshFoundPackages() {
        startOperation()

        val currentSearchTerm = searchTerm.value
        val currentSelectedProjectModule = selectedProjectModule.value

        val packagesMatchingSearchTerm = installedPackages.value
                .filter {
                    it.value.isInstalled && it.value.isInstalledInProjectModule(currentSelectedProjectModule) &&
                            (it.value.identifier.contains(currentSearchTerm, true) ||
                                    it.value.remoteInfo?.name?.contains(currentSearchTerm, true) ?: false)
                }.toMutableMap()

        refreshDependencyLicensesInfo()

        searchResults.set(packagesMatchingSearchTerm)
        searchResultsUpdated.fire(packagesMatchingSearchTerm)
        finishOperation()
    }


    private fun autoRefreshContext() {
        try {
            if (!isBusy.value) {
                refreshContext()
            }
        } finally {
            refreshContextAlarm.cancelAllRequests()
            refreshContextAlarm.addRequest(::autoRefreshContext, refreshContextAlarmInterval)
        }
    }

    private fun refreshContext() {
        refreshPackagesContext()
    }

    private fun refreshPackagesContext() {
        val installedPackagesMap = installedPackages.value.toMutableMap()

        val projectModulesList = mutableListOf<ProjectModule>()

        // Mark all packages as "no longer installed"
        for (entry in installedPackagesMap) {
            entry.value.installationInformation.clear()
        }

        // Fetch all project modules
        val modules = ModuleManager.getInstance(project).modules.toList()
        for (module in modules) {
            // Fetch all packages that are installed in the project and re-populate our map
            val projectModule = ProjectModule(module.name, module)

            ModuleRootManager.getInstance(
                    module
            ).orderEntries().forEachLibrary { library: Library ->
                val identifier = library.getSimpleIdentifier()
                if (identifier != null) {
                    val item = installedPackagesMap.getOrPut(
                            identifier,
                            {
                                LicenseDetectorDependency(
                                        identifier.substringBefore(':'),
                                        identifier.substringAfterLast(':')
                                )
                            }
                    )

                    item.installationInformation.add(
                            InstallationInformation(
                                    projectModule,
                                    library.getVersion()
                            )
                    )
                }
                true
            }

            // Update list of project modules
            projectModulesList.add(projectModule)
        }

        // Any packages that are no longer installed?
        installedPackagesMap.filterNot { it.value.isInstalled }
                .keys
                .forEach { keyToRemove -> installedPackagesMap.remove(keyToRemove) }

        installedPackages.set(installedPackagesMap)
        projectModules.set(projectModulesList)
    }

    private fun refreshDependencyLicensesInfo() {
        startOperation()
        application.executeOnPooledThread {
            val installedPackagesToCheck = installedPackages.value

            if (installedPackagesToCheck.any()) isFetchingSuggestions.set(true)

            installedPackagesToCheck.values.chunked(SearchClient.maxRequestResultsCount).forEach { chunk ->
                val result = searchClient.packagesByRange(chunk.map { "${it.groupId}:${it.artifactId}" })
                if (result.isRight()) {
                    (result as Either.Right).b.packages?.forEach {
                        val simpleIdentifier = it.toSimpleIdentifier()
                        val installedPackage = installedPackages.value[simpleIdentifier]
                        if (installedPackage != null && simpleIdentifier == installedPackage.identifier) {
                            installedPackage.remoteInfo = it
                        }
                    }
                }
            }

            application.invokeLater {
                refreshFoundPackages()
                //TODO: ???
                isFetchingSuggestions.set(false)
                finishOperation()
            }

            finishOperation()
        }
    }

    private fun LicenseDetectorDependency.isInstalledInProjectModule(projectModule: ProjectModule?): Boolean {
        return projectModule == null ||
                this.installationInformation.any { installationInformation ->
                    installationInformation.projectModule == projectModule
                }
    }
}
