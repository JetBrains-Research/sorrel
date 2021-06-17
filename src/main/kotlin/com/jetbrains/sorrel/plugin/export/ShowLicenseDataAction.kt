package com.jetbrains.sorrel.plugin.export

import com.intellij.ide.actions.RevealFileAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import java.io.File

class ShowLicenseDataAction(private val file: File) : AnAction(RevealFileAction.getActionName()), DumbAware {
    override fun actionPerformed(e: AnActionEvent) {
        RevealFileAction.openFile(file)
    }

    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        presentation.isEnabled = file.exists() && RevealFileAction.isSupported()
    }
}