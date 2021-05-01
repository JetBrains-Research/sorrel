package com.jetbrains.licensedetector.intellij.plugin.model

import com.intellij.ProjectTopics
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore.findModuleForFile
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessModuleDir
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
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Function
import com.jetbrains.licensedetector.intellij.plugin.detection.DetectorManager
import com.jetbrains.licensedetector.intellij.plugin.detection.DetectorManager.licenseFileNamePattern
import com.jetbrains.licensedetector.intellij.plugin.licenses.NoLicense
import com.jetbrains.licensedetector.intellij.plugin.licenses.SupportedLicense
import com.jetbrains.licensedetector.intellij.plugin.model.ModuleUtils.getTopLevelModule
import com.jetbrains.licensedetector.intellij.plugin.packagesearch.api.SearchClient
import com.jetbrains.licensedetector.intellij.plugin.packagesearch.api.ServerURLs
import com.jetbrains.licensedetector.intellij.plugin.utils.*
import com.jetbrains.licensedetector.intellij.plugin.utils.TraceInfo
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.IPropertyView
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rd.util.reactive.Signal
import kotlinx.coroutines.*
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import java.util.*

private const val DATA_DEBOUNCE_MILLIS = 100L
private const val SEARCH_DEBOUNCE_MILLIS = 200L

internal class ToolWindowModel(val project: Project) : Disposable {

    private val mainScope = MainScope() + CoroutineName("ToolWindowModel")

    private var dataChangeJob: Job? = null
    private var queryChangeJob: Job? = null

    val lifetime: Lifetime = createLifetime()

    private val searchClient = SearchClient(ServerURLs.base)

    // Observables
    private val _status = Property(DataStatus())
    val status: IPropertyView<DataStatus> = _status

    val searchQuery = Property("")
    val installedPackages = Property(mapOf<String, PackageDependency>())

    val rootModule: Property<ProjectModule?> = Property(null)
    val projectModules = Property(listOf<ProjectModule>())

    val selectedProjectModule = Property<ProjectModule?>(null)
    val selectedPackage = Property("")

    val licenseManager = LicenseManager(lifetime, rootModule, projectModules, installedPackages)

    // UI Signals
    val requestRefreshContext = Signal<Unit>()
    val searchResultsUpdated = Signal<Map<String, PackageDependency>>()


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
                "installedPackages changed (${it.map { it.key }})"
            }
            refreshFoundPackages(searchQuery.value, traceInfo)
        }
        selectedProjectModule.advise(lifetime) {
            val traceInfo = TraceInfo(TraceInfo.TraceSource.SELECTED_MODULE_CHANGES)
            logDebug(traceInfo) {
                "selectedProjectModule changed ($it)"
            }
            refreshFoundPackages(searchQuery.value, traceInfo)
        }
        searchQuery.advise(lifetime) {
            val traceInfo = TraceInfo(TraceInfo.TraceSource.SEARCH_TERM_CHANGES)
            logDebug(traceInfo) {
                "searchTerm changed ($it)"
            }
            refreshFoundPackages(it, traceInfo)
        }

        requestRefreshContext.advise(lifetime) {
            val traceInfo = TraceInfo(TraceInfo.TraceSource.REQUESTS_REFRESH_CONTEXT)
            logDebug(traceInfo) {
                "Refresh context requested"
            }
            refreshContext(traceInfo)
        }

        subscribeOnProjectNotifications()
        subscribeOnModulesNotifications()

        logDebug(initTraceInfo) {
            "Rrefresh context in ToolWindowModel initialization"
        }
        refreshContext(initTraceInfo)

        logDebug(initTraceInfo) { "ToolWindowModel initialization finished" }
    }

    private fun subscribeOnProjectNotifications() {
        project.messageBus.connect().subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                val traceInfo = TraceInfo(TraceInfo.TraceSource.PROJECT_ROOT_CHANGES)
                logDebug(traceInfo) {
                    "Project root changed"
                }
                refreshContext(traceInfo)
            }
        })
    }

    private fun subscribeOnModulesNotifications() {
        project.messageBus.connect().subscribe(ProjectTopics.MODULES, object : ModuleListener {
            override fun moduleAdded(p: Project, module: Module) {
                val traceInfo = TraceInfo(TraceInfo.TraceSource.NEW_MODULE_ADDED)
                logDebug(traceInfo) {
                    "New module added"
                }
                refreshContext(traceInfo)
            }

            override fun moduleRemoved(p: Project, module: Module) {
                val traceInfo = TraceInfo(TraceInfo.TraceSource.MODULE_REMOVED)
                logDebug(traceInfo) {
                    "Old module removed"
                }
                refreshContext(traceInfo)
            }

            override fun modulesRenamed(
                project: Project,
                modules: MutableList<Module>,
                oldNameProvider: Function<Module, String>
            ) {
                val traceInfo = TraceInfo(TraceInfo.TraceSource.EXISTING_MODULE_RENAMED)
                logDebug(traceInfo) {
                    "Existing module renamed"
                }
                refreshContext(traceInfo)
            }
        })
    }

    private fun refreshFoundPackages(query: String, traceInfo: TraceInfo) {
        queryChangeJob?.cancel()
        queryChangeJob = mainScope.launch {
            try {
                delay(SEARCH_DEBOUNCE_MILLIS)
                onFoundPackagesChanged(query, traceInfo)
            } catch (e: CancellationException) {
                logTrace(traceInfo, "onFoundPackagesChanged") { "Execution cancelled: ${e.message}" }
                setStatus(isSearching = false)
            }
        }
    }

    /**
     * Calculate packages that match the selected module and search query and feeds the result to UI component
     */
    private suspend fun onFoundPackagesChanged(query: String, traceInfo: TraceInfo) {
        logDebug(traceInfo) { "Start refresh found packages" }
        if (installedPackages.value.any()) {
            setStatus(isSearching = true)
        }

        val currentSelectedProjectModule = selectedProjectModule.value

        yield()

        val packagesMatchingSearchTerm = installedPackages.value
            .filter {
                it.value.isInstalled && it.value.isInstalledInProjectModule(currentSelectedProjectModule) &&
                        (it.value.identifier.contains(query, true) ||
                                it.value.remoteInfo?.name?.contains(query, true) ?: false)
            }.toMutableMap()

        yield()

        searchResultsUpdated.fire(packagesMatchingSearchTerm)
        setStatus(isSearching = false)
        logDebug(traceInfo) { "Finished refresh found packages" }
    }


    private fun refreshContext(traceInfo: TraceInfo) {
        dataChangeJob?.cancel()
        dataChangeJob = mainScope.launch {
            try {
                delay(DATA_DEBOUNCE_MILLIS)
                onContextChanged(traceInfo)
            } catch (e: CancellationException) {
                logTrace(traceInfo, "onContextChanged") { "Execution cancelled: ${e.message}" }
                setStatus(isRefreshingData = false)
            }
        }
        refreshFoundPackages(searchQuery.value, traceInfo)
    }

    /**
     *  Updates modules and their dependencies, update module license by license file,
     *  then requests remote package information from PackageSearch
     */
    private suspend fun onContextChanged(traceInfo: TraceInfo) {
        setStatus(isRefreshingData = true)
        logDebug(traceInfo) { "Start refresh context" }

        val installedPackagesMap = installedPackages.value.toMutableMap()
        logDebug(traceInfo) { "Old installed packages ${installedPackagesMap.map { it.key }}" }

        val projectModulesList = mutableListOf<ProjectModule>()

        yield()

        // Mark all packages as "no longer installed"
        for (entry in installedPackagesMap) {
            entry.value.installationInformation.clear()
        }

        // Fetch all project modules
        val modules = ModuleManager.getInstance(project).modules.toList()
        logDebug(traceInfo) { "All project modules ${modules.map { it.name }}" }
        for (module in modules) {
            val moduleDir = module.guessModuleDir()
            if (moduleDir == null || !moduleDir.exists() || !moduleDir.isValid) {
                continue
            }

            yield()

            // Fetch all packages that are installed in the project and re-populate our map
            val projectModule = ProjectModule(module.name, module, moduleDir.path)

            val projectLibraries = mutableListOf<Library>()
            ModuleRootManager.getInstance(module).orderEntries().forEachLibrary { library: Library ->
                projectLibraries.add(library)
                true
            }

            projectLibraries.forEach { library ->
                yield()
                val identifier = library.getSimpleIdentifier()
                if (identifier != null) {
                    val item = installedPackagesMap.getOrPut(
                        identifier,
                        {
                            PackageDependency(
                                identifier.substringBefore(':'),
                                identifier.substringAfterLast(':'),
                                licensesFromJarMetaInfo = DetectorManager.getPackageLicensesFromJar(
                                    library,
                                    project
                                )
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
            }

            // Update list of project modules
            projectModulesList.add(projectModule)
        }

        yield()

        // Any packages that are no longer installed?
        installedPackagesMap.filterNot { it.value.isInstalled }
            .keys
            .forEach { keyToRemove -> installedPackagesMap.remove(keyToRemove) }

        installedPackages.set(installedPackagesMap)

        yield()

        projectModules.set(projectModulesList)

        yield()

        val topLevelModule: Module? = project.getTopLevelModule()
        val rootProjectModule: ProjectModule? = projectModules.value.find { it.nativeModule == topLevelModule }
        logDebug(traceInfo) { "Root module is ${rootProjectModule?.name}" }
        rootModule.set(rootProjectModule)

        yield()

        logDebug(traceInfo) { "Update modules licenses by license files" }
        updateModulesLicensesByLicenseFile(traceInfo, projectModulesList)

        yield()

        // Receive packages remote info from PackageSearch
        logDebug(traceInfo) { "Refresh dependency licenses info" }
        refreshPackageDependencyLicensesInfo(traceInfo)

        setStatus(isRefreshingData = false)
        logDebug(traceInfo) { "Finish refresh context" }
    }

    private suspend fun updateModulesLicensesByLicenseFile(
        traceInfo: TraceInfo,
        projectModuleList: List<ProjectModule>
    ) {
        logDebug(traceInfo) { "Start updateModulesLicensesByLicenseFile()" }
        val licenseFiles = getAllLicenseFilesByExts(project)

        yield()

        val moduleLicenseByLicenseFiles: MutableMap<ProjectModule, SupportedLicense> = mutableMapOf()

        for (module in projectModuleList) {
            moduleLicenseByLicenseFiles[module] = NoLicense
        }

        for (licenseFile in licenseFiles) {
            yield()
            val licensePsiFileText = ReadAction.compute<String?, Throwable> {
                PsiManager.getInstance(project).findFile(licenseFile)?.text
            } ?: continue
            val module = findModuleForFile(licenseFile, project) ?: continue
            val projectModule = projectModuleList.find { it.nativeModule == module } ?: continue
            val detectedLicense = DetectorManager.getLicenseByFullText(licensePsiFileText)
            if (detectedLicense != NoLicense) {
                moduleLicenseByLicenseFiles[projectModule] = detectedLicense
            }
        }
        licenseManager.modulesLicenses.set(moduleLicenseByLicenseFiles)
        logDebug(traceInfo) { "Finish updateModulesLicensesByLicenseFile()" }
    }

    private fun getAllLicenseFilesByExts(project: Project): Collection<VirtualFile> {
        val files: MutableList<VirtualFile> = ArrayList()

        val allFilenames = ReadAction.compute<Array<String>, Throwable> {
            FilenameIndex.getAllFilenames(project)
        }

        for (name in allFilenames) {
            if (licenseFileNamePattern.matches(name)) {
                val licenseFiles = ReadAction.compute<Collection<VirtualFile>, Throwable> {
                    FilenameIndex.getVirtualFilesByName(project, name, GlobalSearchScope.allScope(project))
                }

                files.addAll(licenseFiles.filter { !it.isDirectory })
            }
        }
        return files
    }

    /**
     *  Requests remote package information from PackageSearch and invokes the refreshFoundPackages method
     */
    private suspend fun refreshPackageDependencyLicensesInfo(traceInfo: TraceInfo) {
        logDebug(traceInfo) { "Start refresh package dependency licenses info" }
        val installedPackagesToCheck = installedPackages.value

        val installedPackageIdentifiersForRequest = installedPackagesToCheck.values.map { it.identifier }

        val result = searchClient.packagesInfoByRange(installedPackageIdentifiersForRequest)

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
        onFoundPackagesChanged(searchQuery.value, traceInfo)
        logDebug(traceInfo) { "Finish refresh package dependency licenses info" }
    }

    private fun setStatus(isSearching: Boolean? = null, isRefreshingData: Boolean? = null) {
        mainScope.launch {
            val traceInfo = TraceInfo(TraceInfo.TraceSource.STATUS_CHANGES)
            val currentStatus = _status.value
            val newStatus = currentStatus.copy(
                isSearching = isSearching ?: currentStatus.isSearching,
                isRefreshingData = isRefreshingData ?: currentStatus.isRefreshingData,
            )

            if (currentStatus == newStatus) {
                logDebug(traceInfo, "ToolWindowModel#setStatus()") {
                    "Ignoring status change (not really changed)"
                }
                return@launch
            }

            _status.set(newStatus)
            logDebug(traceInfo, "ToolWindowModel#setStatus()") { "Status changed: $newStatus" }
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
        dataChangeJob?.cancel("Disposing service")
        queryChangeJob?.cancel("Disposing service")
        mainScope.cancel("Disposing service")
    }
}
