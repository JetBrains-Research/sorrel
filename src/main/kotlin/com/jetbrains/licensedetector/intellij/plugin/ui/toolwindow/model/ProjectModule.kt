package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile

data class ProjectModule(
        val name: String,
        val nativeModule: Module,
        val parent: ProjectModule?,
        val buildFile: VirtualFile,
) {

    fun getFullName(): String {
        if (parent != null) {
            return parent.getFullName() + ":$name"
        }
        return name
    }
}
