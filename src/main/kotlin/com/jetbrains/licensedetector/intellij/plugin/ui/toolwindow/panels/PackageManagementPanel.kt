package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.ui.AutoScrollToSourceHandler
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorToolWindowModel
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.left.PackagesSmartPanel
import javax.swing.JComponent

class PackageManagementPanel(private val viewModel: LicenseDetectorToolWindowModel) :
        PackageSearchPanelBase(LicenseDetectorBundle.message("licensedetector.ui.toolwindow.tab.packages.title")) {

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

    private val autoScrollToSourceHandler = object : AutoScrollToSourceHandler() {
        override fun isAutoScrollMode(): Boolean {
            return true
            //TODO: What is it?
            //return PackageSearchGeneralConfiguration.getInstance(viewModel.project).autoScrollToSource
        }

        override fun setAutoScrollMode(state: Boolean) {
            //TODO: What is it?
            //PackageSearchGeneralConfiguration.getInstance(viewModel.project).autoScrollToSource = state
        }
    }


    private val mainSplitter = PackagesSmartPanel(viewModel, autoScrollToSourceHandler).content

    override fun build() = mainSplitter

    override fun buildToolbar(): JComponent? {
        val actionGroup = DefaultActionGroup(
                refreshAction,
                Separator(),
                //TODO: Implement settings
                //ShowSettingsAction(viewModel.project),
                autoScrollToSourceHandler.createToggleAction()
        )

        return ActionManager.getInstance().createActionToolbar("", actionGroup, false).component
    }
}
