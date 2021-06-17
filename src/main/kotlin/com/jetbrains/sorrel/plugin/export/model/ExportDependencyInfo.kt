package com.jetbrains.sorrel.plugin.export.model

data class ExportDependencyInfo(
    val name: String,
    val mainLicenseName: ExportLicenseInfo?,
    val otherLicensesNames: List<ExportLicenseInfo>
)
