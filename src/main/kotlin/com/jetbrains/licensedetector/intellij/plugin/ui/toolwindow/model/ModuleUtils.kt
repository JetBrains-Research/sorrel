package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessModuleDir


object ModuleUtils {

    fun Project.hasOneTopLevelModule(): Boolean {
        val modules = ModuleManager.getInstance(this).modules
        val moduleDirPath = modules.mapNotNull { it.guessModuleDir()?.canonicalPath }
        return moduleDirPath.any { path ->
            moduleDirPath.all { it.startsWith(path) }
        }
    }

    fun Project.getTopLevelModule(): Module? {
        val modules = ModuleManager.getInstance(this).modules
        val moduleWithDirPath = modules.mapNotNull {
            val modulePath = it.guessModuleDir()?.canonicalPath
            if (modulePath != null) {
                Pair(it, modulePath)
            } else {
                null
            }
        }
        return moduleWithDirPath.find { pair ->
            moduleWithDirPath.all { it.second.startsWith(pair.second) }
        }?.first
    }

}