package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model

data class CompatibilityIssueData(
    val packageDependencyLicenseIssues: List<String>,
    val submoduleLicenseIssues: List<String>
)
