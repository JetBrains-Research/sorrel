package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model

import com.intellij.ProjectTopics
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.util.Function
import com.jetbrains.licensedetector.intellij.plugin.licenses.SupportedLicense
import com.jetbrains.licensedetector.intellij.plugin.licenses.getCompatiblePackageLicenses
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

    private val searchClient = SearchClient(ServerURLs.base)
    private val operationsCounter = AtomicInteger(0)

    // Observables
    val isBusy = Property(false)
    val isSearching = Property(false)

    val searchTerm = Property("")
    private val installedPackages = Property(mapOf<String, LicenseDetectorDependency>())

    val projectModules = Property(listOf<ProjectModule>())

    val selectedProjectModule = Property<ProjectModule?>(null)
    val selectedPackage = Property("")

    val projectLicensesCompatibleWithPackageLicenses = Property<List<SupportedLicense>>(listOf())

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
        // - search term changes
        installedPackages.advise(lifetime) {
            refreshFoundPackages()
        }
        selectedProjectModule.advise(lifetime) {
            refreshFoundPackages()
        }
        searchTerm.advise(lifetime) {
            refreshFoundPackages()
        }

        requestRefreshContext.advise(lifetime) {
            refreshPackagesContext()
        }

        subscribeOnProjectNotifications()
        subscribeOnModulesNotifications()

        refreshPackagesContext()
    }

    private fun subscribeOnProjectNotifications() {
        project.messageBus.connect().subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                refreshPackagesContext()
            }
        })
    }

    private fun subscribeOnModulesNotifications() {
        project.messageBus.connect().subscribe(ProjectTopics.MODULES, object : ModuleListener {
            override fun moduleAdded(p: Project, module: Module) {
                refreshPackagesContext()
            }

            override fun moduleRemoved(p: Project, module: Module) {
                refreshPackagesContext()
            }

            override fun modulesRenamed(project: Project, modules: MutableList<Module>, oldNameProvider: Function<Module, String>) {
                refreshPackagesContext()
            }
        })
    }

    /**
     * Calculate packages that match the selected module and search query and feeds the result to UI component
     */

    @Suppress("ComplexMethod")
    private fun refreshFoundPackages() {
        startOperation()

        if (installedPackages.value.any()) isSearching.set(true)

        val currentSearchTerm = searchTerm.value
        val currentSelectedProjectModule = selectedProjectModule.value

        val packagesMatchingSearchTerm = installedPackages.value
                .filter {
                    it.value.isInstalled && it.value.isInstalledInProjectModule(currentSelectedProjectModule) &&
                            (it.value.identifier.contains(currentSearchTerm, true) ||
                                    it.value.remoteInfo?.name?.contains(currentSearchTerm, true) ?: false)
                }.toMutableMap()

        searchResultsUpdated.fire(packagesMatchingSearchTerm)
        isSearching.set(false)
        finishOperation()
    }

    /**
     *  Updates modules and their dependencies,
     *  then requests remote package information from PackageSearch
     */

    private fun refreshPackagesContext() {
        startOperation()
        isSearching.set(true)

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

        // Receive packages remote info from PackageSearch
        refreshDependencyLicensesInfo()

        finishOperation()
    }

    /**
     *  Requests remote package information from PackageSearch and invokes the refreshFoundPackages method
     *  @see com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorToolWindowModel.refreshFoundPackages
     */

    private fun refreshDependencyLicensesInfo() {
        startOperation()
        application.executeOnPooledThread {
            val installedPackagesToCheck = installedPackages.value

            val result = searchClient.packagesInfoByRange(installedPackagesToCheck.values.map {
                "${it.groupId}:${it.artifactId}"
            })
            result.forEach {
                val simpleIdentifier = it.toSimpleIdentifier()
                val installedPackage = installedPackages.value[simpleIdentifier]
                if (installedPackage != null && simpleIdentifier == installedPackage.identifier) {
                    installedPackage.remoteInfo = it
                }
            }

            // Update project licenses compatibility with packages licenses
            updateProjectLicensesCompatibilityWithPackagesLicenses()

            // refresh found packages after receiving remote package info
            refreshFoundPackages()
            isSearching.set(false)
            finishOperation()
        }
    }


    private fun updateProjectLicensesCompatibilityWithPackagesLicenses() {
        val licensesAllPackages = mutableSetOf<SupportedLicense>()
        installedPackages.value.values.forEach { dependency ->
            //Add main package license to set
            val mainPackageLicense = dependency.remoteInfo?.licenses?.mainLicense
            if (mainPackageLicense is SupportedLicense) {
                licensesAllPackages.add(mainPackageLicense)
            }

            //Add other package licenses to set
            dependency.remoteInfo?.licenses?.otherLicenses?.forEach {
                if (it is SupportedLicense) {
                    licensesAllPackages.add(it)
                }
            }
        }

        projectLicensesCompatibleWithPackageLicenses.set(getCompatiblePackageLicenses(licensesAllPackages))
    }

    private fun LicenseDetectorDependency.isInstalledInProjectModule(projectModule: ProjectModule?): Boolean {
        return projectModule == null ||
                this.installationInformation.any { installationInformation ->
                    installationInformation.projectModule == projectModule
                }
    }
}
