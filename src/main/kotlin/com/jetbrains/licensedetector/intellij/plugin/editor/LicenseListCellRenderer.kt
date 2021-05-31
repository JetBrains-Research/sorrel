package com.jetbrains.licensedetector.intellij.plugin.editor

import com.intellij.icons.AllIcons.Actions.Cancel
import com.intellij.icons.AllIcons.Actions.Commit
import com.intellij.ui.components.JBLabel
import com.jetbrains.licensedetector.intellij.plugin.licenses.SupportedLicense
import com.jetbrains.licensedetector.intellij.plugin.model.ProjectModule
import com.jetbrains.licensedetector.intellij.plugin.model.ToolWindowModel
import java.awt.Component
import javax.swing.JList
import javax.swing.ListCellRenderer

internal class LicenseListCellRenderer(
    private val model: ToolWindowModel,
    private val projectModule: ProjectModule
) : ListCellRenderer<SupportedLicense> {
    override fun getListCellRendererComponent(
        list: JList<out SupportedLicense>?,
        value: SupportedLicense?,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        return if (value != null) {
            if (model.licenseManager.modulesCompatibleLicenses.value[projectModule]?.contains(value) == true) {
                JBLabel(value.name, Commit, JBLabel.LEFT)
            } else {
                JBLabel(value.name, Cancel, JBLabel.LEFT)
            }
        } else {
            JBLabel("")
        }
    }
}