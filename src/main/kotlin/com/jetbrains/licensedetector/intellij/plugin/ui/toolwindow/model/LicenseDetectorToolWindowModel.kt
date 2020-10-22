package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model

import arrow.core.Either
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.createNestedDisposable
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.util.Alarm
import com.jetbrains.licensedetector.intellij.plugin.getSimpleIdentifier
import com.jetbrains.packagesearch.intellij.plugin.api.SearchClient
import com.jetbrains.packagesearch.intellij.plugin.api.ServerURLs
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rd.util.reactive.Signal

class LicenseDetectorToolWindowModel(val project: Project, val lifetime: Lifetime) {

    private val application = ApplicationManager.getApplication()
    private val parentDisposable = lifetime.createNestedDisposable()

    private val searchClient = SearchClient(ServerURLs.base)
    private val refreshContextAlarmInterval: Long = 10000 // ms
    private val refreshContextAlarm = Alarm(parentDisposable)

    // Observables
    val isBusy = Property(false)
    val isFetchingSuggestions = Property(false)
    val upgradeCountInContext = Property(0)

    private val installedPackages = Property(mapOf<String, LicenseDetectorDependency>())

    //TODO: Use ProjectModule
    //val projectModules = Property(listOf<ProjectModule>())
    val projectModules = Property(listOf<Module>())

    // UI Signals
    val requestRefreshContext = Signal<Boolean>()
    val searchResultsUpdated = Signal<Map<String, LicenseDetectorDependency>>()

    // Implementation
    init {
        // Fetch installed packages and available project modules automatically, and when requested
        val delayMillis = 250
        refreshContextAlarm.addRequest(::autoRefreshContext, delayMillis)
        requestRefreshContext.advise(lifetime) {
            refreshContext()
        }
    }


    private fun autoRefreshContext() {
        try {
            if (!isBusy.value) {
                refreshContext()
                installedPackages.value.forEach {
                    println(it.value.groupId + " " + it.value.artifactId)
                    println(it.value.remoteInfo)
                }
            }
        } finally {
            refreshContextAlarm.cancelAllRequests()
            refreshContextAlarm.addRequest(::autoRefreshContext, refreshContextAlarmInterval)
        }
    }

    private fun refreshContext() {
        refreshPackagesContext()
        refreshDependencyLicensesInfo()
    }

    private fun refreshPackagesContext() {
        val installedPackagesMap = installedPackages.value.toMutableMap()
        //TODO: Use ProjectModule
        //val projectModulesList = mutableListOf<ProjectModule>()
        val projectModulesList = mutableListOf<Module>()

        //TODO: Need info of installing

        // Mark all packages as "no longer installed"
        //installedPackagesMap.clear()

        // Fetch all project modules
        val modules = ModuleManager.getInstance(project).modules.toList()
        for (module in modules) {
            // Fetch all packages that are installed in the project and re-populate our map
            ModuleRootManager.getInstance(
                    module
            ).orderEntries().forEachLibrary { library: Library ->
                val identifier = library.getSimpleIdentifier()
                if (identifier != null) {
                    installedPackagesMap.getOrPut(
                            identifier,
                            {
                                LicenseDetectorDependency(
                                        identifier.substringBefore(':'),
                                        identifier.substringAfterLast(':')
                                )
                            }
                    )
                }
                true
            }

            // Update list of project modules
            projectModulesList.add(module)
        }

        installedPackages.set(installedPackagesMap)
        projectModules.set(projectModulesList)

        //TODO: Fix
        searchResultsUpdated.fire(installedPackagesMap)
    }

    private fun refreshDependencyLicensesInfo() {
        application.executeOnPooledThread {
            val installedPackagesToCheck = installedPackages.value

            if (installedPackagesToCheck.any()) isFetchingSuggestions.set(true)

            installedPackagesToCheck.values.chunked(SearchClient.maxRequestResultsCount).forEach { chunk ->
                println(chunk.map { "${it.groupId}:${it.artifactId}" })
                val result = searchClient.packagesByRange(chunk.map { "${it.groupId}:${it.artifactId}" })
                if (result.isRight()) {
                    (result as Either.Right).b.packages?.forEach {
                        val simpleIdentifier = it.toSimpleIdentifier()
                        val installedPackage = installedPackages.value[simpleIdentifier]
                        if (installedPackage != null && simpleIdentifier == installedPackage.identifier) {
                            installedPackage.remoteInfo = it
                        }
                        println(installedPackages.value[simpleIdentifier]?.remoteInfo)
                    }
                }
            }


            //installedPackages.set(installedPackagesToCheck)

            application.invokeLater {
                //TODO: What is it?
                //refreshFoundPackages()
                isFetchingSuggestions.set(false)
            }
            println(installedPackages.value.values.toTypedArray()[0])
        }
    }

    /*
    private fun searchByName(query: String, onlyStable: Boolean, onlyMpp: Boolean, repositoryIds: List<String>) {
        val searchRequestId = lastSearchRequestId.incrementAndGet()

        searchAlarm.cancelAllRequests()
        searchAlarm.addRequest({
            if (searchRequestId != lastSearchRequestId.get()) return@addRequest

            startOperation()
            isSearching.set(true)
            PackageSearchEventsLogger.onSearchRequest(project, query)

            application.executeOnPooledThread {
                val packageSearchQuery = PackageSearchQuery(query)
                val entries = searchClient.packagesByQuery(packageSearchQuery, onlyStable, onlyMpp, repositoryIds)

                application.invokeLater {
                    if (searchRequestId != lastSearchRequestId.get()) {
                        isSearching.set(false)
                        finishOperation()
                        return@invokeLater
                    }

                    entries.fold(
                            {
                                PackageSearchEventsLogger.onSearchFailed(project, query)

                                NotificationGroup.balloonGroup(PACKAGE_SEARCH_NOTIFICATION_GROUP_ID)
                                        .createNotification(
                                                PackageSearchBundle.message("packagesearch.title"),
                                                PackageSearchBundle.message("packagesearch.search.client.searching.failed"),
                                                it,
                                                NotificationType.ERROR
                                        )
                                        .notify(project)
                            },
                            {
                                PackageSearchEventsLogger.onSearchResponse(project, query, it.packages!!)

                                remotePackages.set(it.packages)
                                isSearching.set(false)
                                finishOperation()
                            })
                }
            }
        }, searchDelay)
    }

     */
}
