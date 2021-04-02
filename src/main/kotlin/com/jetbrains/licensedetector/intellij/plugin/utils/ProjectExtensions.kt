package com.jetbrains.licensedetector.intellij.plugin.utils

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.licensedetector.intellij.plugin.model.ToolWindowModel

internal fun Project.licenseDetectorModel() = service<ToolWindowModel>()
