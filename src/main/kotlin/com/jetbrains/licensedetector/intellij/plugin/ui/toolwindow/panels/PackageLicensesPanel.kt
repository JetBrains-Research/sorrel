package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorToolWindowModel
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.left.PackageLicensesSmartPanel
import javax.swing.JComponent

class PackageLicensesPanel(private val viewModel: LicenseDetectorToolWindowModel) :
        PackageLicensesPanelBase(LicenseDetectorBundle.message("licensedetector.ui.toolwindow.tab.packages.title")) {

    private val refreshAction =
            object : AnAction(
                    LicenseDetectorBundle.message("licensedetector.ui.toolwindow.actions.reload.text"),
                    LicenseDetectorBundle.message("licensedetector.ui.toolwindow.actions.reload.description"),
                    AllIcons.Actions.Refresh
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    viewModel.requestRefreshContext.fire(true)
                }
            }

    private val mainPanel = PackageLicensesSmartPanel(viewModel).content

    override fun build() = mainPanel

    override fun buildToolbar(): JComponent? {
        val actionGroup = DefaultActionGroup(
                refreshAction,
                Separator(),
                //TODO: Implement settings
                //ShowSettingsAction(viewModel.project),
        )

        return ActionManager.getInstance().createActionToolbar("", actionGroup, false).component
    }
}
