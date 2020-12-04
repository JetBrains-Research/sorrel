package com.jetbrains.licensedetector.intellij.plugin.notification

import com.intellij.icons.AllIcons.Actions.Cancel
import com.intellij.icons.AllIcons.Actions.Commit
import com.intellij.ui.components.JBLabel
import com.jetbrains.licensedetector.intellij.plugin.licenses.SupportedLicense
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorToolWindowModel
import java.awt.Component
import javax.swing.JList
import javax.swing.ListCellRenderer

class LicenseListCellRenderer(val model: LicenseDetectorToolWindowModel) : ListCellRenderer<SupportedLicense> {
    override fun getListCellRendererComponent(
            list: JList<out SupportedLicense>?,
            value: SupportedLicense?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean): Component {
        return if (value != null) {
            if (model.projectLicensesCompatibleWithPackageLicenses.value.contains(value)) {
                JBLabel(value.name, Commit, JBLabel.LEFT)
            } else {
                JBLabel(value.name, Cancel, JBLabel.LEFT)
            }
        } else {
            JBLabel("")
        }
    }
}