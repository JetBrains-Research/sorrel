package com.jetbrains.licensedetector.intellij.plugin.notification

import com.jetbrains.licensedetector.intellij.plugin.licenses.SupportedLicense
import java.awt.Component
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.ListCellRenderer

class LicenseListCellRenderer : ListCellRenderer<SupportedLicense> {
    override fun getListCellRendererComponent(
            list: JList<out SupportedLicense>?,
            value: SupportedLicense?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean): Component? {
        return if (value != null) {
            JLabel(value.name)
        } else {
            null
        }
    }
}