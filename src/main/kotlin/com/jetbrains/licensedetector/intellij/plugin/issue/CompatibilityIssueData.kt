package com.jetbrains.licensedetector.intellij.plugin.issue

import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle

data class CompatibilityIssueData(
    val packageDependencyLicenseIssueGroups: List<PackageDependencyIssueGroup>,
    val submoduleLicenseIssueGroups: List<SubmoduleIssueGroup>
) {

    fun isEmpty(): Boolean {
        return packageDependencyLicenseIssueGroups.isEmpty() && submoduleLicenseIssueGroups.isEmpty()
    }

    fun convertCompatibilityIssuesDataToPlainText(): String {
        val stringBuilder = StringBuilder()
        if (packageDependencyLicenseIssueGroups.isNotEmpty()) {
            stringBuilder.append(
                convertPackageDependencyIssueGroupsToPlainText()
            )
        }
        if (submoduleLicenseIssueGroups.isNotEmpty()) {
            stringBuilder.append(
                convertSubmodulesIssueGroupsToPlainText()
            )
        }
        return stringBuilder.toString()
    }

    private fun convertPackageDependencyIssueGroupsToPlainText(): String {
        val stringBuilder = StringBuilder()
        for (group in packageDependencyLicenseIssueGroups) {
            stringBuilder.append(
                LicenseDetectorBundle.message(
                    "licensedetector.ui.compatibilityIssues.plainText.moduleAndDependency.head",
                    group.moduleName,
                    group.moduleLicenseName
                )
            )
            group.issues.forEach { packageDependencyIssue ->
                stringBuilder.append(
                    LicenseDetectorBundle.message(
                        "licensedetector.ui.compatibilityIssues.plainText.moduleAndDependency",
                        packageDependencyIssue.packageIdentifier,
                        packageDependencyIssue.licenseName
                    )
                )
                stringBuilder.append(", ")
            }
            stringBuilder.delete(stringBuilder.length - 2, stringBuilder.length - 1)
        }
        return stringBuilder.toString()
    }

    private fun convertSubmodulesIssueGroupsToPlainText(): String {
        val stringBuilder = StringBuilder()
        for (group in submoduleLicenseIssueGroups) {
            stringBuilder.append(
                LicenseDetectorBundle.message(
                    "licensedetector.ui.compatibilityIssues.plainText.moduleAndSubmodules.head",
                    group.moduleName,
                    group.moduleLicenseName
                )
            )
            group.issues.forEach {
                stringBuilder.append(
                    LicenseDetectorBundle.message(
                        "licensedetector.ui.compatibilityIssues.plainText.moduleAndSubmodules",
                        it.moduleName,
                        it.licenseName
                    )
                )
                stringBuilder.append(", ")
            }
            stringBuilder.delete(stringBuilder.length - 2, stringBuilder.length - 1)
        }
        return stringBuilder.toString()
    }

    fun convertCompatibilityIssuesDataToHtml(): String {
        val stringBuilder = StringBuilder("<html><body><ol>")
        if (packageDependencyLicenseIssueGroups.isNotEmpty()) {
            stringBuilder.append(
                convertPackageDependencyIssueGroupsToHtml()
            )
        }
        if (submoduleLicenseIssueGroups.isNotEmpty()) {
            stringBuilder.append(
                convertSubmodulesIssueGroupsToHtml()
            )
        }
        stringBuilder.append("</ol></body></html>")
        return stringBuilder.toString()
    }

    private fun convertPackageDependencyIssueGroupsToHtml(): String {
        val stringBuilder = StringBuilder()

        for (group in packageDependencyLicenseIssueGroups) {
            stringBuilder.append("<li>")
            stringBuilder.append(
                LicenseDetectorBundle.message(
                    "licensedetector.ui.compatibilityIssues.html.moduleAndDependency.head",
                    group.moduleName,
                    group.moduleLicenseName
                )
            )
            stringBuilder.append("<ul>")
            group.issues.forEach { packageDependencyIssue ->
                stringBuilder.append("<li>")
                stringBuilder.append(
                    LicenseDetectorBundle.message(
                        "licensedetector.ui.compatibilityIssues.html.moduleAndDependency",
                        packageDependencyIssue.packageIdentifier,
                        packageDependencyIssue.licenseName
                    )
                )
                stringBuilder.append("</li>")
            }
            stringBuilder.append("</ul>")
            stringBuilder.append("</li>")
        }

        return stringBuilder.toString()
    }

    private fun convertSubmodulesIssueGroupsToHtml(): String {
        val stringBuilder = StringBuilder()

        for (group in submoduleLicenseIssueGroups) {
            stringBuilder.append("<li>")
            stringBuilder.append(
                LicenseDetectorBundle.message(
                    "licensedetector.ui.compatibilityIssues.html.moduleAndSubmodules.head",
                    group.moduleName,
                    group.moduleLicenseName
                )
            )
            stringBuilder.append("<ul>")
            group.issues.forEach {
                stringBuilder.append("<li>")
                stringBuilder.append(
                    LicenseDetectorBundle.message(
                        "licensedetector.ui.compatibilityIssues.html.moduleAndSubmodules",
                        it.moduleName,
                        it.licenseName
                    )
                )
                stringBuilder.append("</li>")
            }
            stringBuilder.append("</ul>")
            stringBuilder.append("</li>")
        }

        return stringBuilder.toString()
    }
}
