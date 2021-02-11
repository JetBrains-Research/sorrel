package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model

import com.intellij.openapi.project.guessModuleDir
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.licenses.ALL_SUPPORTED_LICENSE
import com.jetbrains.licensedetector.intellij.plugin.licenses.NoLicense
import com.jetbrains.licensedetector.intellij.plugin.licenses.SupportedLicense
import com.jetbrains.licensedetector.intellij.plugin.licenses.getCompatiblePackageLicenses
import com.jetbrains.licensedetector.intellij.plugin.module.ProjectModule
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.Property
import org.jetbrains.kotlin.utils.keysToMap
import java.nio.file.Paths

class LicenseManager(
    lifetime: Lifetime,
    val rootModule: Property<ProjectModule?>,
    private val projectModules: Property<List<ProjectModule>>,
    private val installedPackages: Property<Map<String, LicenseDetectorDependency>>
) {
    val modulesLicenses: Property<Map<ProjectModule, SupportedLicense>> =
        Property(projectModules.value.keysToMap { NoLicense })
    val modulesCompatibleLicenses = Property(mapOf<ProjectModule, List<SupportedLicense>>())

    private val moduleLicensesCompatibleWithPackageLicenses = Property(mapOf<ProjectModule, Set<SupportedLicense>>())

    //TODO: Update when ml model is ready
    private val moduleLicenseCompatibleWithSourceLicenses = Property(mapOf<ProjectModule, Set<SupportedLicense>>())

    val compatibilityIssues = Property<List<String>>(listOf())

    init {
        updatingModulesCompatibleLicenses(lifetime)
    }

    private fun updatingModulesCompatibleLicenses(lifetime: Lifetime) {
        moduleLicensesCompatibleWithPackageLicenses.advise(lifetime) { mapLicenses ->
            val newModulesCompatibleLicenses: MutableMap<ProjectModule, List<SupportedLicense>> = mutableMapOf()
            for ((module, setLicense) in mapLicenses) {
                //TODO: Replace ALL_SUPPORTED_LICENSE when ml model is ready
                newModulesCompatibleLicenses[module] = setLicense.intersect(ALL_SUPPORTED_LICENSE.toSet())
                    .toList().sortedByDescending { it.priority }
            }

            modulesCompatibleLicenses.set(newModulesCompatibleLicenses)
        }

        moduleLicenseCompatibleWithSourceLicenses.advise(lifetime) { mapLicenses ->
            val newModulesCompatibleLicenses: MutableMap<ProjectModule, List<SupportedLicense>> = mutableMapOf()
            val curModuleLicensesCompatibleWithPackageLicenses = moduleLicensesCompatibleWithPackageLicenses.value
            for ((module, setLicense) in mapLicenses) {
                curModuleLicensesCompatibleWithPackageLicenses[module] ?: continue

                newModulesCompatibleLicenses[module] = setLicense
                    .intersect(curModuleLicensesCompatibleWithPackageLicenses[module]!!)
                    .toList().sortedByDescending { it.priority }
            }

            modulesCompatibleLicenses.set(newModulesCompatibleLicenses)
        }

        modulesLicenses.advise(lifetime) { newMap ->
            checkCompatibilityWithModuleLicenses(newMap, installedPackages.value.values)
        }

        projectModules.advise(lifetime) { moduleList ->
            val newModuleLicenses: MutableMap<ProjectModule, SupportedLicense> = modulesLicenses.value.toMutableMap()
            moduleList.forEach {
                newModuleLicenses.putIfAbsent(it, NoLicense)
            }
            modulesLicenses.set(newModuleLicenses)
        }
    }

    fun updateProjectLicensesCompatibilityWithPackagesLicenses(
        modules: List<ProjectModule>,
        packages: Collection<LicenseDetectorDependency>
    ) {
        val newModuleLicensesCompatibleWithPackageLicenses: MutableMap<ProjectModule, Set<SupportedLicense>> =
            mutableMapOf()

        var hasCompatibilityIssues = false

        for (module in modules) {
            val licensesAllPackages = packages.fold(mutableSetOf<SupportedLicense>()) { acc, dependency ->
                if (dependency.installationInformation.any { it.projectModule == module }) {
                    //Add main package license to set
                    val mainPackageLicense = dependency.remoteInfo?.licenses?.mainLicense
                    if (mainPackageLicense is SupportedLicense) {
                        acc.add(mainPackageLicense)
                    }

                    //Add other package licenses to set
                    dependency.remoteInfo?.licenses?.otherLicenses?.forEach {
                        if (it is SupportedLicense) {
                            acc.add(it)
                        }
                    }
                }

                acc
            }

            val compatibleProjectLicenses = getCompatiblePackageLicenses(licensesAllPackages)
            newModuleLicensesCompatibleWithPackageLicenses[module] = compatibleProjectLicenses

            if (!hasCompatibilityIssues && !compatibleProjectLicenses.contains(modulesLicenses.value[module])) {
                hasCompatibilityIssues = true
            }
        }

        moduleLicensesCompatibleWithPackageLicenses.set(newModuleLicensesCompatibleWithPackageLicenses)

        if (hasCompatibilityIssues) {
            checkCompatibilityWithModuleLicenses(modulesLicenses.value, packages)
        }
    }

    private fun checkCompatibilityWithModuleLicenses(
        modulesLicenses: Map<ProjectModule, SupportedLicense>,
        packages: Collection<LicenseDetectorDependency>
    ) {
        val issuesList = mutableListOf<String>()

        for ((module, license) in modulesLicenses) {
            val inheritedLicense = getModuleLicense(modulesLicenses, module, license)
            var hasCompatibleIssue = false
            val stringBuilder = StringBuilder(
                LicenseDetectorBundle.message(
                    "licensedetector.ui.compatibilityIssues.projectAndDependency.head",
                    module.name
                )
            )
            stringBuilder.append("<ul>")

            // Add compatibility issues if needed
            packages.forEach { dependency ->
                if (dependency.installationInformation.any { it.projectModule == module }) {
                    val mainPackageLicense = dependency.remoteInfo?.licenses?.mainLicense
                    if (mainPackageLicense is SupportedLicense &&
                        !mainPackageLicense.compatibleDependencyLicenses.contains(inheritedLicense)
                    ) {
                        hasCompatibleIssue = true
                        stringBuilder.append(
                            "<li>" +
                                    LicenseDetectorBundle.message(
                                        "licensedetector.ui.compatibilityIssues.projectAndDependency",
                                        dependency.identifier,
                                        mainPackageLicense.name,
                                        inheritedLicense.name
                                    ) + "</li>"
                        )
                    }
                    dependency.remoteInfo?.licenses?.otherLicenses?.forEach {
                        if (it is SupportedLicense &&
                            !it.compatibleDependencyLicenses.contains(inheritedLicense)
                        ) {
                            hasCompatibleIssue = true
                            stringBuilder.append(
                                "<li>" +
                                        LicenseDetectorBundle.message(
                                            "licensedetector.ui.compatibilityIssues.projectAndDependency",
                                            dependency.identifier,
                                            it.name,
                                            inheritedLicense.name
                                        ) + "</li>"
                            )
                        }
                    }
                }
            }
            stringBuilder.append("</ul>")

            if (hasCompatibleIssue) {
                issuesList.add(stringBuilder.toString())
            }
        }

        return if (issuesList.isNotEmpty()) {
            compatibilityIssues.set(issuesList)
        } else {
            compatibilityIssues.set(listOf())
        }
    }

    private fun getModuleLicense(
        modulesLicenses: Map<ProjectModule, SupportedLicense>,
        projectModule: ProjectModule,
        moduleLicense: SupportedLicense
    ): SupportedLicense {
        if (moduleLicense != NoLicense) {
            return moduleLicense
        }

        val projectModulePath = Paths.get(
            projectModule.nativeModule.guessModuleDir()?.canonicalPath ?: return moduleLicense
        )

        val modulesWithDirPath = modulesLicenses.map { entry ->
            Pair(entry.key, Paths.get(entry.key.nativeModule.guessModuleDir()?.canonicalPath ?: ""))
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