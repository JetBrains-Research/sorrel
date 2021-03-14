package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model

import com.intellij.ProjectTopics
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore.findModuleForFile
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.createLifetime
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex.getAllFilesByExt
import com.intellij.util.Function
import com.jetbrains.licensedetector.intellij.plugin.detection.DetectorManager
import com.jetbrains.licensedetector.intellij.plugin.detection.DetectorManager.licenseFileNamePattern
import com.jetbrains.licensedetector.intellij.plugin.licenses.License
import com.jetbrains.licensedetector.intellij.plugin.licenses.NoLicense
import com.jetbrains.licensedetector.intellij.plugin.licenses.SupportedLicense
import com.jetbrains.licensedetector.intellij.plugin.module.ProjectModule
import com.jetbrains.licensedetector.intellij.plugin.packagesearch.api.SearchClient
import com.jetbrains.licensedetector.intellij.plugin.packagesearch.api.ServerURLs
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.ModuleUtils.getTopLevelModule
import com.jetbrains.licensedetector.intellij.plugin.utils.TraceInfo
import com.jetbrains.licensedetector.intellij.plugin.utils.getSimpleIdentifier
import com.jetbrains.licensedetector.intellij.plugin.utils.getVersion
import com.jetbrains.licensedetector.intellij.plugin.utils.logDebug
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rd.util.reactive.Signal
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import java.util.concurrent.atomic.AtomicInteger

class ToolWindowModel(val project: Project) : Disposable {

    private val application = ApplicationManager.getApplication()

    val lockObject = Any()
    val lifetime: Lifetime = createLifetime()

    private val searchClient = SearchClient(ServerURLs.base)
    private val operationsCounter = AtomicInteger(0)

    // Observables
    val isBusy = Property(false)
    val isSearching = Property(false)

    val searchTerm = Property("")
    val installedPackages = Property(mapOf<String, PackageDependency>())

    val rootModule: Property<ProjectModule?> = Property(null)
    val projectModules = Property(listOf<ProjectModule>())

    val selectedProjectModule = Property<ProjectModule?>(null)
    val selectedPackage = Property("")

    val licenseManager = LicenseManager(lockObject, lifetime, rootModule, projectModules, installedPackages)

    // UI Signals
    val requestRefreshContext = Signal<Unit>()
    val searchResultsUpdated = Signal<Map<String, PackageDependency>>()

    private fun startOperation() {
        isBusy.set(operationsCounter.incrementAndGet() > 0)
    }

    private fun finishOperation() {
        isBusy.set(operationsCounter.decrementAndGet() > 0)
    }


    // Implementation
    init {
        val initTraceInfo = TraceInfo(TraceInfo.TraceSource.INIT_MAIN_MODEL)
        logDebug(initTraceInfo) { "ToolWindowModel initialization started" }
        // Populate foundPackages when either:
        // - list of installed packages changes
        // - selected module changes
        // - search term changes
        installedPackages.advise(lifetime) {
            val traceInfo = TraceInfo(TraceInfo.TraceSource.PACKAGE_DEPENDENCY_LIST_CHANGES)
            logDebug(traceInfo) {
                "installedPackages changed (${it.map { it.key }}), pooled thread requested to found package data refresh"
            }
            application.executeOnPooledThread {
                logDebug(traceInfo) {
                    "Started found package data refresh for installedPackages changes (${it.map { it.key }})"
                }

                refreshFoundPackages(traceInfo)

                logDebug(traceInfo) {
                    "Completed found package data refresh for installedPackages changes (${it.map { it.key }})"
                }
            }
        }
        selectedProjectModule.advise(lifetime) {
            val traceInfo = TraceInfo(TraceInfo.TraceSource.SELECTED_MODULE_CHANGES)
            logDebug(traceInfo) {
                "selectedProjectModule changed ($it), pooled thread requested to found package data refresh"
            }
            application.executeOnPooledThread {
                logDebug(traceInfo) {
                    "Started found package data refresh for selectedProjectModule changes ($it)"
                }

                refreshFoundPackages(traceInfo)

                logDebug(traceInfo) {
                    "Completed found package data refresh for selectedProjectModule changes ($it)"
                }
            }
        }
        searchTerm.advise(lifetime) {
            val traceInfo = TraceInfo(TraceInfo.TraceSource.SEARCH_TERM_CHANGES)
            logDebug(traceInfo) {
                "searchTerm changed ($it), pooled thread requested to found package data refresh"
            }
            application.executeOnPooledThread {
                logDebug(traceInfo) {
                    "Started found package data refresh for searchTerm changes ($it)"
                }

                refreshFoundPackages(traceInfo)

                logDebug(traceInfo) {
                    "Completed found package data refresh for searchTerm changes ($it)"
                }
            }
        }

        requestRefreshContext.advise(lifetime) {
            val traceInfo = TraceInfo(TraceInfo.TraceSource.REQUESTS_REFRESH_CONTEXT)
            logDebug(traceInfo) {
                "Refresh context requested, pooled thread requested to refresh context"
            }

            application.executeOnPooledThread {
                logDebug(traceInfo) {
                    "Context refresh started"
                }

                refreshContext(traceInfo)

                logDebug(traceInfo) {
                    "Context refresh completed"
                }
            }
        }

        subscribeOnProjectNotifications()
        subscribeOnModulesNotifications()
        subscribeOnVFSChanges()

        logDebug(initTraceInfo) {
            "Pooled thread requested to refresh context in ToolWindowModel initialization"
        }
        application.executeOnPooledThread {
            logDebug(initTraceInfo) {
                "Context refresh started"
            }

            refreshContext(initTraceInfo)

            logDebug(initTraceInfo) {
                "Context refresh completed"
            }
        }

        logDebug(initTraceInfo) { "ToolWindowModel initialization finished" }
    }

    private fun subscribeOnProjectNotifications() {
        project.messageBus.connect().subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                val traceInfo = TraceInfo(TraceInfo.TraceSource.PROJECT_ROOT_CHANGES)
                logDebug(traceInfo) {
                    "Project root changed, pooled thread requested to refresh context"
                }

                application.executeOnPooledThread {
                    logDebug(traceInfo) {
                        "Context refresh started"
                    }

                    //refreshContext(traceInfo)

                    logDebug(traceInfo) {
                        "Context refresh completed"
                    }
                }
            }
        })
    }

    private fun subscribeOnModulesNotifications() {
        project.messageBus.connect().subscribe(ProjectTopics.MODULES, object : ModuleListener {
            override fun moduleAdded(p: Project, module: Module) {
                val traceInfo = TraceInfo(TraceInfo.TraceSource.NEW_MODULE_ADDED)
                logDebug(traceInfo) {
                    "New module added, pooled thread requested to refresh context"
                }
                application.executeOnPooledThread {
                    logDebug(traceInfo) {
                        "Context refresh started"
                    }

                    //refreshContext(traceInfo)

                    logDebug(traceInfo) {
                        "Context refresh completed"
                    }
                }
            }

            override fun moduleRemoved(p: Project, module: Module) {
                val traceInfo = TraceInfo(TraceInfo.TraceSource.MODULE_REMOVED)
                logDebug(traceInfo) {
                    "Old module removed, pooled thread requested to refresh context"
                }

                application.executeOnPooledThread {
                    logDebug(traceInfo) {
                        "Context refresh started"
                    }

                    //refreshContext(traceInfo)

                    logDebug(traceInfo) {
                        "Context refresh completed"
                    }
                }
            }

            override fun modulesRenamed(
                project: Project,
                modules: MutableList<Module>,
                oldNameProvider: Function<Module, String>
            ) {
                val traceInfo = TraceInfo(TraceInfo.TraceSource.EXISTING_MODULE_RENAMED)
                logDebug(traceInfo) {
                    "Existing module renamed, pooled thread requested to refresh context"
                }

                application.executeOnPooledThread {
                    logDebug(traceInfo) {
                        "Context refresh started"
                    }

                    //refreshContext(traceInfo)

                    logDebug(traceInfo) {
                        "Context refresh completed"
                    }
                }
            }
        })
    }


    private fun subscribeOnVFSChanges() {
        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                val traceInfo = TraceInfo(TraceInfo.TraceSource.VFS_CHANGES)
                logDebug(traceInfo) {
                    "VFS changed, pooled thread requested to refresh context"
                }
                application.executeOnPooledThread {
                    logDebug(traceInfo) {
                        "Context refresh started"
                    }

                    synchronized(lockObject) {
                        //updateModuleLicenseByVFSEvents(traceInfo, events)
                    }

                    logDebug(traceInfo) {
                        "Context refresh completed"
                    }
                }
            }
        })
    }

    // Update module license in accordance with VFS changes in the license file
    private fun updateModuleLicenseByVFSEvents(traceInfo: TraceInfo, events: MutableList<out VFileEvent>) {
        val moduleLicenseByLicenseFiles: MutableMap<ProjectModule, SupportedLicense> =
            mutableMapOf()
        val currentModuleLicenses = licenseManager.modulesLicenses.value

        logDebug(traceInfo) {
            "Modules licenses before update $currentModuleLicenses"
        }

        val currentProjectModules = projectModules.value
        for ((module, license) in currentModuleLicenses) {
            moduleLicenseByLicenseFiles[module] = license
        }

        for (event in events) {
            val updatedFile = event.file
            if (updatedFile != null && licenseFileNamePattern.matches(updatedFile.name) &&
                !updatedFile.isDirectory && updatedFile.isValid
            ) {
                val licensePsiText = ReadAction.compute<String, Throwable> {
                    if (updatedFile.isValid) {
                        updatedFile.toPsiFile(project)?.text
                    } else {
                        null
                    }
                } ?: continue
                val module = findModuleForFile(updatedFile, project) ?: continue
                val projectModule =
                    currentProjectModules.find { it.nativeModule == module } ?: continue
                val detectedLicense = DetectorManager.getLicenseByFullText(licensePsiText)
                moduleLicenseByLicenseFiles[projectModule] = detectedLicense
            }
        }

        logDebug(traceInfo) {
            "Modules licenses after update $currentModuleLicenses"
        }

        licenseManager.modulesLicenses.set(moduleLicenseByLicenseFiles)
    }

    /**
     * Calculate packages that match the selected module and search query and feeds the result to UI component
     */

    @Suppress("ComplexMethod")
    internal fun refreshFoundPackages(traceInfo: TraceInfo) {
        logDebug(traceInfo) { "Started refresh found packages. Entering a synchronized section" }
        synchronized(lockObject) {
            logDebug(traceInfo) { "Start refresh found packages in the synchronized section" }
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
            logDebug(traceInfo) { "Finish refresh found packages in the synchronized section" }
        }
        logDebug(traceInfo) { "Finished refresh found packages. Exit a synchronized section" }
    }

    /**
     *  Updates modules and their dependencies, update module license by license file,
     *  then requests remote package information from PackageSearch
     */
    private fun refreshContext(traceInfo: TraceInfo) {
        logDebug(traceInfo) { "Started refresh context. Entering a synchronized section" }
        synchronized(lockObject) {
            logDebug(traceInfo) { "Start refresh context in the synchronized section" }
            startOperation()
            isSearching.set(true)

            val installedPackagesMap = installedPackages.value.toMutableMap()
            logDebug(traceInfo) { "Old installed packages ${installedPackagesMap.map { it.key }}" }

            val projectModulesList = mutableListOf<ProjectModule>()

            // Mark all packages as "no longer installed"
            for (entry in installedPackagesMap) {
                entry.value.installationInformation.clear()
            }

            // Fetch all project modules
            val modules = ModuleManager.getInstance(project).modules.toList()
            logDebug(traceInfo) { "All project modules ${modules.map { it.name }}" }
            for (module in modules) {
                // Fetch all packages that are installed in the project and re-populate our map
                val projectModule = ProjectModule(module.name, module)

                ModuleRootManager.getInstance(module).orderEntries().forEachLibrary { library: Library ->
                    val identifier = library.getSimpleIdentifier()
                    if (identifier != null) {
                        val item = installedPackagesMap.getOrPut(
                            identifier,
                            {
                                PackageDependency(
                                    identifier.substringBefore(':'),
                                    identifier.substringAfterLast(':'),
                                    licensesFromJarMetaInfo = ReadAction.compute<Set<License>, Throwable> {
                                        DetectorManager.getPackageLicensesFromJar(
                                            library,
                                            project
                                        )
                                    }
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
                    logDebug(traceInfo) { "Package ${library.name} processed" }
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

            val topLevelModule: Module? = project.getTopLevelModule()
            val rootProjectModule: ProjectModule? = projectModules.value.find { it.nativeModule == topLevelModule }
            logDebug(traceInfo) { "Root module is ${rootProjectModule?.name}" }
            rootModule.set(rootProjectModule)

            logDebug(traceInfo) { "Update modules licenses by license files" }
            updateModulesLicensesByLicenseFile(traceInfo, projectModulesList)

            // Receive packages remote info from PackageSearch
            logDebug(traceInfo) { "Refresh dependency licenses info" }
            refreshPackageDependencyLicensesInfo(traceInfo)

            finishOperation()
            logDebug(traceInfo) { "Finish refresh context in the synchronized section" }
        }
        logDebug(traceInfo) { "Finished refresh context. Exit a synchronized section" }
    }

    private fun updateModulesLicensesByLicenseFile(traceInfo: TraceInfo, projectModuleList: List<ProjectModule>) {
        logDebug(traceInfo) { "Started updateModulesLicensesByLicenseFile(). Entering a synchronized section" }
        synchronized(lockObject) {
            logDebug(traceInfo) { "Start updateModulesLicensesByLicenseFile() in the synchronized section" }
            //TODO: cannot find proper way to get all project files by regex
            // It will probably run very slowly on real projects.
            // One option is to capture the set of names to be found.
            // Then it will be possible to use indices and get an acceleration of 1000x.
            val licenseFiles = ReadAction.compute<Collection<VirtualFile>, Throwable> {
                getAllFilesByExt(project, "txt") +
                        getAllFilesByExt(project, "md") +
                        getAllFilesByExt(project, "html") +
                        getAllFilesByExt(project, "")
            }
            val moduleLicenseByLicenseFiles: MutableMap<ProjectModule, SupportedLicense> = mutableMapOf()

            for (module in projectModuleList) {
                moduleLicenseByLicenseFiles[module] = NoLicense
            }

            for (licenseFile in licenseFiles) {
                if (licenseFileNamePattern.matches(licenseFile.name) && !licenseFile.isDirectory) {
                    val licensePsiFileText = ReadAction.compute<String?, Throwable> {
                        PsiManager.getInstance(project).findFile(licenseFile)?.text
                    } ?: continue
                    val module = findModuleForFile(licenseFile, project) ?: continue
                    val projectModule = projectModuleList.find { it.nativeModule == module } ?: continue
                    val detectedLicense = DetectorManager.getLicenseByFullText(licensePsiFileText)
                    moduleLicenseByLicenseFiles[projectModule] = detectedLicense
                }
            }
            licenseManager.modulesLicenses.set(moduleLicenseByLicenseFiles)
            logDebug(traceInfo) { "Finish updateModulesLicensesByLicenseFile() in the synchronized section" }
        }
        logDebug(traceInfo) { "Finished updateModulesLicensesByLicenseFile(). Exit a synchronized section" }
    }

    /**
     *  Requests remote package information from PackageSearch and invokes the refreshFoundPackages method
     *  @see com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.ToolWindowModel.refreshFoundPackages
     */

    private fun refreshPackageDependencyLicensesInfo(traceInfo: TraceInfo) {
        application.executeOnPooledThread {
            logDebug(traceInfo) { "Started refresh package dependency licenses info. Entering a synchronized section" }
            synchronized(lockObject) {
                logDebug(traceInfo) { "Start refresh package dependency licenses info in the synchronized section" }
                startOperation()
                val installedPackagesToCheck = installedPackages.value

                val result = searchClient.packagesInfoByRange(installedPackagesToCheck.values.map {
                    it.identifier
                })
                result.forEach {
                    val simpleIdentifier = it.toSimpleIdentifier()
                    val installedPackage = installedPackages.value[simpleIdentifier]
                    if (installedPackage != null && simpleIdentifier == installedPackage.identifier) {
                        installedPackage.remoteInfo = it
                    }
                }

                licenseManager.updateModuleLicensesCompatibilityWithPackagesLicenses(
                    licenseManager.modulesLicenses.value,
                    installedPackagesToCheck.values
                )
                licenseManager.checkCompatibilityWithPackageDependencyLicenses(
                    licenseManager.modulesLicenses.value,
                    installedPackagesToCheck.values
                )

                // refresh found packages after receiving remote package info
                refreshFoundPackages(traceInfo)
                isSearching.set(false)
                finishOperation()
                logDebug(traceInfo) { "Finish refresh package dependency licenses info in the synchronized section" }
            }
            logDebug(traceInfo) { "Finished refresh package dependency licenses info. Exit a synchronized section" }
        }
    }


    private fun PackageDependency.isInstalledInProjectModule(projectModule: ProjectModule?): Boolean {
        return projectModule == null ||
                this.installationInformation.any { installationInformation ->
                    installationInformation.projectModule == projectModule
                }
    }

    override fun dispose() {
        logDebug { "Disposing ToolWindowModel..." }
    }
}
