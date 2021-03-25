package com.jetbrains.licensedetector.intellij.plugin.issue

data class SubmoduleIssueGroup(
    val moduleName: String,
    val moduleLicenseName: String,
    val issues: List<SubmoduleIssue>
)
