package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model

import com.intellij.ProjectTopics
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore.findModuleForFile
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.createNestedDisposable
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex.getAllFilesByExt
import com.intellij.util.Function
import com.jetbrains.licensedetector.intellij.plugin.detection.DetectorManager
import com.jetbrains.licensedetector.intellij.plugin.detection.DetectorManager.licenseFileNamePattern
import com.jetbrains.licensedetector.intellij.plugin.licenses.NoLicense
import com.jetbrains.licensedetector.intellij.plugin.licenses.SupportedLicense
import com.jetbrains.licensedetector.intellij.plugin.module.ProjectModule
import com.jetbrains.licensedetector.intellij.plugin.packagesearch.api.SearchClient
import com.jetbrains.licensedetector.intellij.plugin.packagesearch.api.ServerURLs
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.ModuleUtils.getTopLevelModule
import com.jetbrains.licensedetector.intellij.plugin.utils.getSimpleIdentifier
import com.jetbrains.licensedetector.intellij.plugin.utils.getVersion
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rd.util.reactive.Signal
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import java.util.concurrent.atomic.AtomicInteger

class ToolWindowModel(val project: Project, val lifetime: Lifetime) {

    private val application = ApplicationManager.getApplication()

    val lockObject = Any()

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
    val requestRefreshContext = Signal<Boolean>()
    val searchResultsUpdated = Signal<Map<String, PackageDependency>>()

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
            application.executeOnPooledThread {
                refreshFoundPackages()
            }
        }
        selectedProjectModule.advise(lifetime) {
            application.executeOnPooledThread {
                refreshFoundPackages()
            }
        }
        searchTerm.advise(lifetime) {
            application.executeOnPooledThread {
                refreshFoundPackages()
            }
        }

        requestRefreshContext.advise(lifetime) {
            application.executeOnPooledThread {
                refreshContext()
            }
        }

        subscribeOnProjectNotifications()
        subscribeOnModulesNotifications()
        subscribeOnVFSChanges()
        subscribeOnDocumentChanges()

        application.executeOnPooledThread {
            refreshContext()
        }
    }

    private fun subscribeOnProjectNotifications() {
        project.messageBus.connect().subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                application.executeOnPooledThread {
                    refreshContext()
                }
            }
        })
    }

    private fun subscribeOnModulesNotifications() {
        project.messageBus.connect().subscribe(ProjectTopics.MODULES, object : ModuleListener {
            override fun moduleAdded(p: Project, module: Module) {
                application.executeOnPooledThread {
                    refreshContext()
                }
            }

            override fun moduleRemoved(p: Project, module: Module) {
                application.executeOnPooledThread {
                    refreshContext()
                }
            }

            override fun modulesRenamed(
                project: Project,
                modules: MutableList<Module>,
                oldNameProvider: Function<Module, String>
            ) {
                application.executeOnPooledThread {
                    refreshContext()
                }
            }
        })
    }


    private fun subscribeOnVFSChanges() {
        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: MutableList<out VFileEvent>) {
                application.executeOnPooledThread {
                    updateModuleLicenseByVFSEvents(events)
                }
            }
        })
    }

    // Update module license in accordance with VFS changes in the license file
    private fun updateModuleLicenseByVFSEvents(events: MutableList<out VFileEvent>) {
        val moduleLicenseByLicenseFiles: MutableMap<ProjectModule, SupportedLicense> =
            mutableMapOf()
        val currentModuleLicenses = licenseManager.modulesLicenses.value
        val currentProjectModules = projectModules.value
        for ((module, license) in currentModuleLicenses) {
            moduleLicenseByLicenseFiles[module] = license
        }

        for (event in events) {
            val updatedFile = event.file
            if (updatedFile != null && licenseFileNamePattern.matches(updatedFile.name) &&
                !updatedFile.isDirectory
            ) {
                val licensePsiFile = updatedFile.toPsiFile(project) ?: continue
                val module = findModuleForFile(updatedFile, project) ?: continue
                val projectModule =
                    currentProjectModules.find { it.nativeModule == module } ?: continue
                val detectedLicense = DetectorManager.getLicenseByFullText(licensePsiFile.text)
                moduleLicenseByLicenseFiles[projectModule] = detectedLicense
            }
        }
        licenseManager.modulesLicenses.set(moduleLicenseByLicenseFiles)
    }

    private fun subscribeOnDocumentChanges() {
        EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                application.executeOnPooledThread {
                    updateModuleLicenseByDocumentEvent(event)
                }
            }
        }, lifetime.createNestedDisposable())
    }

    // Update module license in accordance with Document change in the license file
    private fun updateModuleLicenseByDocumentEvent(event: DocumentEvent) {
        val moduleLicenseByLicenseFiles: MutableMap<ProjectModule, SupportedLicense> = mutableMapOf()
        val currentModuleLicenses = licenseManager.modulesLicenses.value
        val currentProjectModules = projectModules.value
        for ((module, license) in currentModuleLicenses) {
            moduleLicenseByLicenseFiles[module] = license
        }

        val updatedFile = PsiDocumentManager.getInstance(project).getPsiFile(event.document)
        if (updatedFile != null && licenseFileNamePattern.matches(updatedFile.name) &&
            !updatedFile.isDirectory
        ) {
            val module = findModuleForFile(updatedFile.virtualFile, project)
            val projectModule = currentProjectModules.find { it.nativeModule == module }
            if (projectModule != null) {
                val detectedLicense = DetectorManager.getLicenseByFullText(updatedFile.text)
                moduleLicenseByLicenseFiles[projectModule] = detectedLicense
            }
        }
    }


    /**
     * Calculate packages that match the selected module and search query and feeds the result to UI component
     */

    @Suppress("ComplexMethod")
    fun refreshFoundPackages() {
        synchronized(lockObject) {
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
    }

    /**
     *  Updates modules and their dependencies, update module license by license file,
     *  then requests remote package information from PackageSearch
     */
    private fun refreshContext() {
        ReadAction.run<Throwable> {
            synchronized(lockObject) {
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

                val topLevelModule: Module = project.getTopLevelModule()
                val rootProjectModule: ProjectModule = projectModules.value.find { it.nativeModule == topLevelModule }!!
                rootModule.set(rootProjectModule)

                updateModulesLicensesByLicenseFile(projectModulesList)

                // Receive packages remote info from PackageSearch
                refreshDependencyLicensesInfo()

                finishOperation()
            }
        }
    }

    private fun updateModulesLicensesByLicenseFile(projectModuleList: List<ProjectModule>) {
        ReadAction.run<Throwable> {
            synchronized(lockObject) {
                //TODO: cannot find proper way to get all project files by regex
                // It will probably run very slowly on real projects.
                // One option is to capture the set of names to be found.
                // Then it will be possible to use indices and get an acceleration of 1000x.
                val licenseFiles = getAllFilesByExt(project, "txt") +
                        getAllFilesByExt(project, "md") +
                        getAllFilesByExt(project, "html") +
                        getAllFilesByExt(project, "")

                val moduleLicenseByLicenseFiles: MutableMap<ProjectModule, SupportedLicense> = mutableMapOf()

                for (module in projectModuleList) {
                    moduleLicenseByLicenseFiles[module] = NoLicense
                }

                for (licenseFile in licenseFiles) {
                    if (licenseFileNamePattern.matches(licenseFile.name) && !licenseFile.isDirectory) {
                        val licensePsiFile = PsiManager.getInstance(project).findFile(licenseFile) ?: continue
                        val module = findModuleForFile(licenseFile, project) ?: continue
                        val projectModule = projectModuleList.find { it.nativeModule == module } ?: continue
                        val detectedLicense = DetectorManager.getLicenseByFullText(licensePsiFile.text)
                        moduleLicenseByLicenseFiles[projectModule] = detectedLicense
                    }
                }
                licenseManager.modulesLicenses.set(moduleLicenseByLicenseFiles)
            }
        }
    }

    /**
     *  Requests remote package information from PackageSearch and invokes the refreshFoundPackages method
     *  @see com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.ToolWindowModel.refreshFoundPackages
     */

    private fun refreshDependencyLicensesInfo() {
        startOperation()
        application.executeOnPooledThread {
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
            refreshFoundPackages()
            isSearching.set(false)
            finishOperation()
        }
    }


    private fun PackageDependency.isInstalledInProjectModule(projectModule: ProjectModule?): Boolean {
        return projectModule == null ||
                this.installationInformation.any { installationInformation ->
                    installationInformation.projectModule == projectModule
                }
    }
}
