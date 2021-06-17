package com.jetbrains.sorrel.plugin.export.model

import com.google.gson.GsonBuilder
import com.intellij.openapi.project.Project
import com.jetbrains.sorrel.plugin.issue.CompatibilityIssueData
import com.jetbrains.sorrel.plugin.utils.licenseDetectorModel

data class ExportLicenseData(
    val rootModule: ExportRootModuleInfo?,
    val modulesInfo: List<ExportModuleInfo>,
    val compatibilityIssues: CompatibilityIssueData
) {
    companion object {
        private fun createCollectedLicenseData(project: Project): ExportLicenseData {
            val model = project.licenseDetectorModel()
            val rootModule = model.rootModule.value
            val licenseManager = model.licenseManager
            val modulesLicenses = licenseManager.modulesLicenses.value
            val rootModuleLicense = modulesLicenses[rootModule]

            val exportRootModule = if (rootModule != null && rootModuleLicense != null) {
                val exportLicenseData = ExportLicenseInfo(rootModuleLicense.name, rootModuleLicense.spdxId)
                ExportRootModuleInfo(rootModule.name, rootModule.path, exportLicenseData)
            } else {
                null
            }

            val projectModules = model.projectModules.value
            val installedPackages = model.installedPackages.value.values
            val mutableModulesInfos = projectModules.map { projectModule ->
                val moduleInstalledPackages = installedPackages.filter { packageDependency ->
                    packageDependency.installationInformation.any {
                        it.projectModule == projectModule
                    }
                }

                val mutableLicenseInfo = moduleInstalledPackages.map { packageDependency ->
                    val mainLicense = packageDependency.getMainLicense()
                    ExportDependencyInfo(
                        packageDependency.identifier,
                        if (mainLicense != null) {
                            ExportLicenseInfo(mainLicense.name, mainLicense.spdxId)
                        } else {
                            null
                        },
                        packageDependency.getOtherLicenses().map {
                            ExportLicenseInfo(it.name, it.spdxId)
                        }
                    )
                }

                ExportModuleInfo(
                    project.name,
                    project.projectFilePath!!,
                    mutableLicenseInfo
                )
            }

            val compatibilityIssueData = licenseManager.compatibilityIssues.value

            return ExportLicenseData(
                exportRootModule,
                mutableModulesInfos,
                compatibilityIssueData
            )
        }

        fun createCollectedLicenseDataJson(project: Project): String {
            val exportLicenseData = createCollectedLicenseData(project)
            val gson = GsonBuilder().setPrettyPrinting().create()
            return gson.toJson(exportLicenseData)
        }
    }
}
