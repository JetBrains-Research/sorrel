package com.jetbrains.sorrel.plugin.toolwindow.panels

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.sorrel.plugin.utils.licenseDetectorModel

class RefreshAction : AnAction(
    com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.toolwindow.actions.reload.text"),
    com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.toolwindow.actions.reload.description"),
    AllIcons.Actions.Refresh
) {
    override fun actionPerformed(e: AnActionEvent) {
        e.project!!.licenseDetectorModel().requestRefreshContext.fire(Unit)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled = project != null && !project.licenseDetectorModel().status.value.isBusy
    }
}