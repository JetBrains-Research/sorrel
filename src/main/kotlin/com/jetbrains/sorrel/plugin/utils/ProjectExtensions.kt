package com.jetbrains.sorrel.plugin.utils

import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.sorrel.plugin.model.ToolWindowModel

internal fun Project.licenseDetectorModel() = service<ToolWindowModel>()
