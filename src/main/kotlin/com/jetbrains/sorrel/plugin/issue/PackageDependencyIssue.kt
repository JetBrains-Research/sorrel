package com.jetbrains.sorrel.plugin.issue

data class PackageDependencyIssue(
    val packageIdentifier: String,
    val licenseName: String
)
