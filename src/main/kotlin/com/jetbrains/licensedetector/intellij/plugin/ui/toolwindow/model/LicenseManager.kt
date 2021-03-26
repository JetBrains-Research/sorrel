package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model

import com.jetbrains.licensedetector.intellij.plugin.issue.*
import com.jetbrains.licensedetector.intellij.plugin.licenses.ALL_SUPPORTED_LICENSE
import com.jetbrains.licensedetector.intellij.plugin.licenses.NoLicense
import com.jetbrains.licensedetector.intellij.plugin.licenses.SupportedLicense
import com.jetbrains.licensedetector.intellij.plugin.licenses.getCompatiblePackageLicenses
import com.jetbrains.licensedetector.intellij.plugin.module.ProjectModule
import com.jetbrains.licensedetector.intellij.plugin.utils.logDebug
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.Property
import com.jetbrains.rd.util.remove
import org.jetbrains.kotlin.utils.keysToMap
import java.nio.file.Path
import java.nio.file.Paths

class LicenseManager(
    lifetime: Lifetime,
    val rootModule: Property<ProjectModule?>,
    private val projectModules: Property<List<ProjectModule>>,
    private val installedPackages: Property<Map<String, PackageDependency>>
) {

    val modulesLicenses: Property<Map<ProjectModule, SupportedLicense>> =
        Property(projectModules.value.keysToMap { NoLicense })
    val modulesCompatibleLicenses: Property<Map<ProjectModule, List<SupportedLicense>>> =
        Property(projectModules.value.keysToMap { listOf() })

    private val moduleLicensesCompatibleWithPackageLicenses = Property(mapOf<ProjectModule, Set<SupportedLicense>>())
    private val moduleLicensesCompatibleWithSubmoduleLicenses = Property(mapOf<ProjectModule, Set<SupportedLicense>>())

    //TODO: Update when ml model is ready
    private val moduleLicenseCompatibleWithSourceLicenses = Property(mapOf<ProjectModule, Set<SupportedLicense>>())

    val compatibilityIssues = Property(CompatibilityIssueData(listOf(), listOf()))

    init {
        updatingModulesCompatibleLicenses(lifetime)
    }

    private fun updatingModulesCompatibleLicenses(lifetime: Lifetime) {
        moduleLicensesCompatibleWithPackageLicenses.advise(lifetime) { mapLicenses ->
            logDebug("LicenseManager: Updating packages licenses")
            val newModulesCompatibleLicenses: MutableMap<ProjectModule, List<SupportedLicense>> = mutableMapOf()
            val curModuleLicenseCompatibleWithSubmoduleLicenses =
                moduleLicensesCompatibleWithSubmoduleLicenses.value
            for ((module, setLicense) in mapLicenses) {
                //TODO: Replace ALL_SUPPORTED_LICENSE when ml model is ready
                if (curModuleLicenseCompatibleWithSubmoduleLicenses.containsKey(module)) {
                    newModulesCompatibleLicenses[module] = setLicense
                        .intersect(ALL_SUPPORTED_LICENSE.toSet())
                        .intersect(curModuleLicenseCompatibleWithSubmoduleLicenses[module]!!)
                        .toList().sortedByDescending { it.priority }
                }
            }

            modulesCompatibleLicenses.set(newModulesCompatibleLicenses)
        }

        moduleLicenseCompatibleWithSourceLicenses.advise(lifetime) { mapLicenses ->
            logDebug("LicenseManager: Updating sources licenses")
            val newModulesCompatibleLicenses: MutableMap<ProjectModule, List<SupportedLicense>> = mutableMapOf()
            val curModuleLicensesCompatibleWithPackageLicenses = moduleLicensesCompatibleWithPackageLicenses.value
            val curModuleLicenseCompatibleWithSubmoduleLicenses =
                moduleLicensesCompatibleWithSubmoduleLicenses.value
            for ((module, setLicense) in mapLicenses) {
                curModuleLicensesCompatibleWithPackageLicenses[module] ?: continue
                curModuleLicenseCompatibleWithSubmoduleLicenses[module] ?: continue
                newModulesCompatibleLicenses[module] = setLicense
                    .intersect(curModuleLicensesCompatibleWithPackageLicenses[module]!!)
                    .intersect(curModuleLicenseCompatibleWithSubmoduleLicenses[module]!!)
                    .toList().sortedByDescending { it.priority }
            }

            modulesCompatibleLicenses.set(newModulesCompatibleLicenses)
        }

        moduleLicensesCompatibleWithSubmoduleLicenses.advise(lifetime) { mapLicenses ->
            logDebug("LicenseManager: Updating submodules licenses")
            val newModulesCompatibleLicenses: MutableMap<ProjectModule, List<SupportedLicense>> = mutableMapOf()
            val curModuleLicensesCompatibleWithPackageLicenses = moduleLicensesCompatibleWithPackageLicenses.value
            for ((module, setLicense) in mapLicenses) {
                //TODO: Replace ALL_SUPPORTED_LICENSE when ml model is ready
                curModuleLicensesCompatibleWithPackageLicenses[module] ?: continue
                newModulesCompatibleLicenses[module] = setLicense
                    .intersect(ALL_SUPPORTED_LICENSE.toSet())
                    .intersect(curModuleLicensesCompatibleWithPackageLicenses[module]!!)
                    .toList().sortedByDescending { it.priority }
            }

            modulesCompatibleLicenses.set(newModulesCompatibleLicenses)
        }

        modulesLicenses.advise(lifetime) { newMap ->
            logDebug("LicenseManager: Updating modules licenses")
            updateModuleLicensesCompatibilityWithSubmoduleLicenses(newMap)
            checkCompatibilityWithSubmodulesLicenses(newMap)
            updateModuleLicensesCompatibilityWithPackagesLicenses(newMap, installedPackages.value.values)
            checkCompatibilityWithPackageDependencyLicenses(newMap, installedPackages.value.values)
        }

        projectModules.advise(lifetime) { moduleList ->
            logDebug("LicenseManager: Updating modules")
            val newModuleLicenses: MutableMap<ProjectModule, SupportedLicense> =
                modulesLicenses.value.toMutableMap()
            //Add new module if module added or renamed
            moduleList.forEach {
                newModuleLicenses.putIfAbsent(it, NoLicense)
            }
            //Delete old module if module removed or renamed
            newModuleLicenses.filter {
                moduleList.contains(it.key)
            }
            modulesLicenses.set(newModuleLicenses)
        }

        installedPackages.advise(lifetime) {
            logDebug("LicenseManager: Updating installed packages")
            updateModuleLicensesCompatibilityWithPackagesLicenses(modulesLicenses.value, it.values)
            checkCompatibilityWithPackageDependencyLicenses(modulesLicenses.value, it.values)
        }
    }

    private fun updateModuleLicensesCompatibilityWithSubmoduleLicenses(
        moduleLicenses: Map<ProjectModule, SupportedLicense>
    ) {
        val newModuleLicensesCompatibleWithSubmoduleLicenses = mutableMapOf<ProjectModule, Set<SupportedLicense>>()
        moduleLicenses.forEach {
            newModuleLicensesCompatibleWithSubmoduleLicenses[it.key] = ALL_SUPPORTED_LICENSE.remove(NoLicense).toSet()
        }

        val projectModulesWithPath = moduleLicenses.mapNotNull {
            val pathString = it.key.path
            Pair(it.key, Paths.get(pathString))
        }

        for (module in moduleLicenses.keys) {
            var compatibleLicenses = ALL_SUPPORTED_LICENSE.remove(NoLicense).toSet()
            val modulePathString = module.path
            val modulePath = Paths.get(modulePathString)

            //find top module with license (not NoLicense)
            var moduleParentPath: Path? = modulePath.parent
            var moduleParent: ProjectModule? = projectModulesWithPath.find { it.second == moduleParentPath }?.first
            while (moduleParentPath != null && (moduleParent == null ||
                        (moduleLicenses[moduleParent] == null || moduleLicenses[moduleParent] == NoLicense))
            ) {

                moduleParentPath = moduleParentPath.parent
                moduleParent = projectModulesWithPath.find { it.second == moduleParentPath }?.first
            }

            // if top module was found then intersect with his compatible licenses
            if (moduleParent != null) {
                val licensesCompatibleWithParentModuleLicense =
                    moduleLicenses[moduleParent]!!.compatibleDependencyLicenses
                compatibleLicenses = compatibleLicenses.intersect(licensesCompatibleWithParentModuleLicense)
            }

            // find and get licenses of all submodules of current modules
            val listOfCompatibleLicenses = getSubmoduleOfModuleWithLicenses(
                module,
                modulePath,
                moduleLicenses,
                projectModulesWithPath
            ).map { pair ->
                pair.second.compatibleModuleLicenses
            }
            listOfCompatibleLicenses.forEach {
                compatibleLicenses = compatibleLicenses.intersect(it)
            }

            newModuleLicensesCompatibleWithSubmoduleLicenses[module] = compatibleLicenses
        }

        moduleLicensesCompatibleWithSubmoduleLicenses.set(newModuleLicensesCompatibleWithSubmoduleLicenses)
    }

    fun updateModuleLicensesCompatibilityWithPackagesLicenses(
        modulesLicenses: Map<ProjectModule, SupportedLicense>,
        packages: Collection<PackageDependency>
    ) {
        val newModuleLicensesCompatibleWithPackageLicenses: MutableMap<ProjectModule, Set<SupportedLicense>> =
            mutableMapOf()

        val projectModulesWithPath = modulesLicenses.mapNotNull {
            val pathString = it.key.path
            Pair(it.key, Paths.get(pathString))
        }

        for ((module, _) in modulesLicenses) {
            val modulePathString = module.path
            val modulePath = Paths.get(modulePathString)

            val submodulesSet = getSubmoduleOfModuleWithNoLicenses(
                module,
                modulePath,
                modulesLicenses,
                projectModulesWithPath
            ).map { it.first }.toSet()

            val licensesAllPackages = packages.fold(mutableSetOf<SupportedLicense>()) { acc, dependency ->
                if (dependency.installationInformation.any {
                        submodulesSet.contains(it.projectModule) || it.projectModule == module
                    }) {
                    //Add main package license to set
                    val mainPackageLicense = dependency.getMainLicense()
                    if (mainPackageLicense is SupportedLicense) {
                        acc.add(mainPackageLicense)
                    }

                    //Add other package licenses to set
                    dependency.getOtherLicenses().forEach {
                        if (it is SupportedLicense) {
                            acc.add(it)
                        }
                    }
                }

                acc
            }

            val compatibleProjectLicenses = getCompatiblePackageLicenses(licensesAllPackages)
            newModuleLicensesCompatibleWithPackageLicenses[module] = compatibleProjectLicenses
        }

        moduleLicensesCompatibleWithPackageLicenses.set(newModuleLicensesCompatibleWithPackageLicenses)
    }

    /**
     * Find incompatibilities between module licenses and their package dependencies and
     * construct textual representations (list items) of the found issues.
     */
    fun checkCompatibilityWithPackageDependencyLicenses(
        modulesLicenses: Map<ProjectModule, SupportedLicense>,
        packages: Collection<PackageDependency>
    ) {
        val groupIssuesList = mutableListOf<PackageDependencyIssueGroup>()

        for ((module, license) in modulesLicenses) {
            val issuesList = mutableListOf<PackageDependencyIssue>()
            val inheritedLicense = getModuleLicense(modulesLicenses, module, license)

            // Add compatibility issues if needed
            packages.forEach { dependency ->
                if (dependency.installationInformation.any { it.projectModule == module }) {
                    val mainPackageLicense = dependency.getMainLicense()
                    if (mainPackageLicense is SupportedLicense &&
                        !mainPackageLicense.compatibleModuleLicenses.contains(inheritedLicense)
                    ) {
                        issuesList.add(PackageDependencyIssue(dependency.identifier, mainPackageLicense.name))
                    }
                    dependency.getOtherLicenses().forEach {
                        if (it is SupportedLicense &&
                            !it.compatibleModuleLicenses.contains(inheritedLicense)
                        ) {
                            issuesList.add(PackageDependencyIssue(dependency.identifier, it.name))
                        }
                    }
                }
            }

            if (issuesList.isNotEmpty()) {
                groupIssuesList.add(PackageDependencyIssueGroup(module.name, inheritedLicense.name, issuesList))
            }
        }

        val previousCompatibilityIssueData = compatibilityIssues.value

        return if (groupIssuesList.isNotEmpty()) {
            compatibilityIssues.set(
                CompatibilityIssueData(
                    groupIssuesList,
                    previousCompatibilityIssueData.submoduleLicenseIssueGroups
                )
            )
        } else {
            compatibilityIssues.set(
                CompatibilityIssueData(
                    listOf(),
                    previousCompatibilityIssueData.submoduleLicenseIssueGroups
                )
            )
        }
    }

    private fun checkCompatibilityWithSubmodulesLicenses(
        moduleLicenses: Map<ProjectModule, SupportedLicense>
    ) {
        val groupIssuesList = mutableListOf<SubmoduleIssueGroup>()

        val projectModulesWithPath = moduleLicenses.mapNotNull {
            val pathString = it.key.path
            Pair(it.key, Paths.get(pathString))
        }

        for ((module, license) in moduleLicenses) {
            val submoduleIssues = mutableListOf<SubmoduleIssue>()

            val modulePathString = module.path
            val modulePath = Paths.get(modulePathString)

            if (license != NoLicense) {

                val listOfCompatibleLicenses = getSubmoduleOfModuleWithLicenses(
                    module,
                    modulePath,
                    moduleLicenses,
                    projectModulesWithPath
                )

                listOfCompatibleLicenses.forEach {
                    if (!it.second.compatibleModuleLicenses.contains(license)) {
                        submoduleIssues.add(SubmoduleIssue(it.first.name, it.second.name))
                    }
                }

                if (submoduleIssues.isNotEmpty()) {
                    groupIssuesList.add(SubmoduleIssueGroup(module.name, license.name, submoduleIssues))
                }
            }
        }

        val previousCompatibilityIssueData = compatibilityIssues.value

        return if (groupIssuesList.isNotEmpty()) {
            compatibilityIssues.set(
                CompatibilityIssueData(
                    previousCompatibilityIssueData.packageDependencyLicenseIssueGroups,
                    groupIssuesList
                )
            )
        } else {
            compatibilityIssues.set(
                CompatibilityIssueData(
                    previousCompatibilityIssueData.packageDependencyLicenseIssueGroups,
                    listOf()
                )
            )
        }
    }

    /**
     * Find all sub-modules of the target module that are not licensed (not No License)
     */
    private fun getSubmoduleOfModuleWithLicenses(
        targetModule: ProjectModule,
        targetModulePath: Path,
        modulesLicenses: Map<ProjectModule, SupportedLicense>,
        projectModulesWithPath: List<Pair<ProjectModule, Path>>
    ): List<Pair<ProjectModule, SupportedLicense>> {
        // find and get licenses of all submodules (without No License) of current modules
        val subModulesWithLicenses = modulesLicenses.toList().filter {
            val curModulePathString = it.first.path
            val curModulePath = Paths.get(curModulePathString)
            it.second != NoLicense && curModulePath.startsWith(targetModulePath)
                    && curModulePath != targetModulePath
        }

        return findSubmodulesOfTargetModule(
            targetModule,
            subModulesWithLicenses,
            modulesLicenses,
            projectModulesWithPath
        )
    }


    private fun getSubmoduleOfModuleWithNoLicenses(
        targetModule: ProjectModule,
        targetModulePath: Path,
        modulesLicenses: Map<ProjectModule, SupportedLicense>,
        projectModulesWithPath: List<Pair<ProjectModule, Path>>
    ): List<Pair<ProjectModule, SupportedLicense>> {
        // find and get licenses of all submodules (only with No License) of current modules
        val subModulesWithLicenses = modulesLicenses.toList().filter {
            val curModulePathString = it.first.path
            val curModulePath = Paths.get(curModulePathString)
            it.second == NoLicense && curModulePath.startsWith(targetModulePath)
                    && curModulePath != targetModulePath
        }

        return findSubmodulesOfTargetModule(
            targetModule,
            subModulesWithLicenses,
            modulesLicenses,
            projectModulesWithPath
        )
    }


    private fun findSubmodulesOfTargetModule(
        targetModule: ProjectModule,
        subModulesWithLicenses: List<Pair<ProjectModule, SupportedLicense>>,
        modulesLicenses: Map<ProjectModule, SupportedLicense>,
        projectModulesWithPath: List<Pair<ProjectModule, Path>>
    ): List<Pair<ProjectModule, SupportedLicense>> {
        return subModulesWithLicenses.filter {
            var subModuleParentPath: Path? = Paths.get(it.first.path).parent
            var subModuleParent: ProjectModule? = projectModulesWithPath.find { mod ->
                mod.second == subModuleParentPath
            }?.first
            while (subModuleParentPath != null && subModuleParent != targetModule &&
                (subModuleParent == null ||
                        (modulesLicenses[subModuleParent] == null ||
                                modulesLicenses[subModuleParent] == NoLicense))
            ) {

                subModuleParentPath = subModuleParentPath.parent
                subModuleParent = projectModulesWithPath.find { mod ->
                    mod.second == subModuleParentPath
                }?.first
            }

            subModuleParent == targetModule
        }
    }

    /**
     * Finds the license of the given module.
     * If it does not exist, then it tries to recursively find the license of the parent module.
     */
    private fun getModuleLicense(
        modulesLicenses: Map<ProjectModule, SupportedLicense>,
        projectModule: ProjectModule,
        moduleLicense: SupportedLicense
    ): SupportedLicense {
        if (moduleLicense != NoLicense) {
            return moduleLicense
        }

        val projectModulePath = Paths.get(projectModule.path)

        val modulesWithDirPath = modulesLicenses.map { entry ->
            Pair(entry.key, Paths.get(entry.key.path))
        }

        var curModuleLicense: SupportedLicense = NoLicense
        var curPath = projectModulePath.parent

        while (curModuleLicense == NoLicense &&
            curPath != null
        ) {

            val parentModule: ProjectModule? = modulesWithDirPath.find { pair ->
                pair.second == curPath
            }?.first

            if (parentModule == null) {
                curPath = curPath.parent
                continue
            }

            val licenseParentModule = modulesLicenses[parentModule]
            if (licenseParentModule == null) {
                curPath = curPath.parent
                continue
            }

            curModuleLicense = licenseParentModule
            curPath = curPath.parent
        }


        return curModuleLicense
    }
}