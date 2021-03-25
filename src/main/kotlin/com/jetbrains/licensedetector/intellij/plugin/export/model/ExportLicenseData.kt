package com.jetbrains.licensedetector.intellij.plugin.export.model

import com.google.gson.Gson
import com.intellij.openapi.project.Project
import com.jetbrains.licensedetector.intellij.plugin.utils.licenseDetectorModel

data class ExportLicenseData(
    val rootModule: ExportRootModuleInfo?,
    val modulesInfo: List<ExportModuleInfo>,
    val compatibilityIssues: String
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
                ExportRootModuleInfo(rootModule.name, rootModule.nativeModule.moduleFilePath, exportLicenseData)
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
                compatibilityIssueData.convertCompatibilityIssuesDataToPlainText()
            )
        }

        fun createCollectedLicenseDataJson(project: Project): String {
            val exportLicenseData = createCollectedLicenseData(project)
            return Gson().toJson(exportLicenseData)
        }
    }
}
