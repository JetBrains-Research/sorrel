package com.jetbrains.licensedetector.intellij.plugin.model

data class InstallationInformation(
    val projectModule: ProjectModule,
    val installedVersion: String?
)
