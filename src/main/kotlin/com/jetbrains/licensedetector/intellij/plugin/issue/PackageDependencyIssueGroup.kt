package com.jetbrains.licensedetector.intellij.plugin.issue

data class PackageDependencyIssueGroup(
    val moduleName: String,
    val moduleLicenseName: String,
    val issues: List<PackageDependencyIssue>
)
