package com.jetbrains.sorrel.plugin.issue

data class PackageDependencyIssueGroup(
    val moduleName: String,
    val moduleLicenseName: String,
    val issues: List<PackageDependencyIssue>
)
