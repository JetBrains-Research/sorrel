package com.jetbrains.licensedetector.intellij.plugin.module

import com.intellij.openapi.module.Module

data class ProjectModule(
    val name: String,
    val nativeModule: Module,
    val path: String
)