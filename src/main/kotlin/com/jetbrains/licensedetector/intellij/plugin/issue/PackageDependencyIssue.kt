package com.jetbrains.licensedetector.intellij.plugin.issue

data class PackageDependencyIssue(
    val packageIdentifier: String,
    val licenseName: String
)
