package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels

import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorToolWindowModel
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.left.PackageLicensesSmartPanel
import javax.swing.JComponent

class PackageLicensesPanel(private val viewModel: LicenseDetectorToolWindowModel) :
        PackageLicensesPanelBase(LicenseDetectorBundle.message("licensedetector.ui.toolwindow.tab.packages.title")) {

    private val mainPanel = PackageLicensesSmartPanel(viewModel).content

    override fun build() = mainPanel

    override fun buildToolbar(): JComponent? = null
}
