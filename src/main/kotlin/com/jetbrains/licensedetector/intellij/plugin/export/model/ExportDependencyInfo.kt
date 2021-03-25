package com.jetbrains.licensedetector.intellij.plugin.export.model

data class ExportDependencyInfo(
    val name: String,
    val mainLicenseName: ExportLicenseInfo?,
    val otherLicensesNames: List<ExportLicenseInfo>
)
