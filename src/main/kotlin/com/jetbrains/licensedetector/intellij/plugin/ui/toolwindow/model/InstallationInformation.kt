package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model

import com.jetbrains.licensedetector.intellij.plugin.module.ProjectModule

data class InstallationInformation(
        val projectModule: ProjectModule,
        val installedVersion: String?)
