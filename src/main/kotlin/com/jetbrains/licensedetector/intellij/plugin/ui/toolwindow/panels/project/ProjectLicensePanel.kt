package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.project

import com.intellij.openapi.project.Project
import com.intellij.util.ui.JBUI
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderUI
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorToolWindowModel
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.PanelBase
import javax.swing.JComponent

class ProjectLicensePanel(
        val project: Project,
        val model: LicenseDetectorToolWindowModel
) : PanelBase(LicenseDetectorBundle.message("licensedetector.ui.toolwindow.tab.project.title")) {


    private val titleView = ProjectTitleView(project, model)


    override fun build(): JComponent = RiderUI.boxPanel {
        border = JBUI.Borders.empty(0, 0, 0, 0)

        add(titleView.panel)

        /*add(PackageSearchUI.boxPanel {
            border = JBUI.Borders.empty(0, 12, 0, 0)

            add(descriptionView.panel)
            add(platformsView.panel)
            add(contentPanel)
            add(Box.createVerticalGlue())
        })*/
    }
}