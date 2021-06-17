package com.jetbrains.sorrel.plugin.model

data class InstallationInformation(
    val projectModule: ProjectModule,
    val installedVersion: String?
)
