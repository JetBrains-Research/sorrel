package com.jetbrains.sorrel.plugin.export.model

class ExportModuleInfo(
    val name: String,
    val path: String,
    val dependencyInfoList: List<ExportDependencyInfo>
)