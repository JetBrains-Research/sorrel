package com.jetbrains.licensedetector.intellij.plugin.export.model

data class ExportRootModuleInfo(
    val name: String,
    val path: String,
    val license: ExportLicenseInfo
)
