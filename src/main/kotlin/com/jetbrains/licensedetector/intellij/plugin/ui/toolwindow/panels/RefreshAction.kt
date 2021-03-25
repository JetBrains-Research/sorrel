package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.utils.licenseDetectorModel

class RefreshAction : AnAction(
    LicenseDetectorBundle.message("licensedetector.ui.toolwindow.actions.reload.text"),
    LicenseDetectorBundle.message("licensedetector.ui.toolwindow.actions.reload.description"),
    AllIcons.Actions.Refresh
) {
    override fun actionPerformed(e: AnActionEvent) {
        e.project!!.licenseDetectorModel().requestRefreshContext.fire(Unit)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled = project != null
    }
}