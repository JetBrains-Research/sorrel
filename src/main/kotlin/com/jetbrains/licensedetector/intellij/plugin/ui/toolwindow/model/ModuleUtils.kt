package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessModuleDir


object ModuleUtils {

    fun Project.hasOneTopLevelModule(): Boolean {
        val modules = ModuleManager.getInstance(this).modules
        val moduleDirPath = modules.map { it.guessModuleDir()?.canonicalPath ?: "" }
        return moduleDirPath.any { path ->
            moduleDirPath.all { it.startsWith(path) }
        }
    }

    fun Project.getTopLevelModule(): Module {
        val modules = ModuleManager.getInstance(this).modules
        val moduleWithDirPath = modules.map { Pair(it, it.guessModuleDir()?.canonicalPath ?: "") }
        return moduleWithDirPath.find { pair ->
            moduleWithDirPath.all { it.second.startsWith(pair.second) }
        }!!.first
    }

}