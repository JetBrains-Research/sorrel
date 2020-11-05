package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.packages.components

import com.intellij.util.IconUtil
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.jetbrains.licensedetector.intellij.plugin.ui.PackageSearchPluginIcons
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderColor
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderUI
import com.jetbrains.licensedetector.intellij.plugin.ui.toHtml
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorDependency
import org.apache.commons.lang3.StringUtils
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.*

private val packageIconSize by lazy { JBUI.scale(16) }
private val packageIcon by lazy { IconUtil.toSize(PackageSearchPluginIcons.Package, packageIconSize, packageIconSize) }

class PackagesSmartRenderer() : ListCellRenderer<PackagesSmartItem> {

    override fun getListCellRendererComponent(
            list: JList<out PackagesSmartItem>,
            item: PackagesSmartItem?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
    ): Component? =
            when (item) {
                is PackagesSmartItem.Package -> {
                    val iconLabel = JLabel(packageIcon).apply {
                        minimumSize = Dimension(packageIconSize, packageIconSize)
                        preferredSize = Dimension(packageIconSize, packageIconSize)
                        maximumSize = Dimension(packageIconSize, packageIconSize)
                    }

                    val packagePanel = createPackagePanel(item.meta, isSelected, list, iconLabel)
                    packagePanel
                }
                is PackagesSmartItem.Header -> item.panel
                is PackagesSmartItem.Fake -> PackagesSmartItem.Fake.panel
                null -> null
            }

    private fun createPackagePanel(
            packageSearchDependency: LicenseDetectorDependency,
            isSelected: Boolean,
            list: JList<out PackagesSmartItem>,
            iconLabel: JLabel
    ): JPanel {
        val textColor = RiderUI.getTextColor(isSelected)
        val textColor2 = RiderUI.getTextColor2(isSelected)

        return buildPanel(
                packageSearchDependency = packageSearchDependency,
                applyColors = applyColors(isSelected, list),
                iconLabel = iconLabel,
                idMessage = buildIdMessage(packageSearchDependency, textColor, textColor2),
                licenseName = packageSearchDependency.remoteInfo?.licenses?.mainLicense?.name ?: ""
        )
    }

    private fun buildIdMessage(
            packageSearchDependency: LicenseDetectorDependency,
            textColor: RiderColor,
            textColor2: RiderColor
    ): String = buildString {
        if (packageSearchDependency.remoteInfo?.name != null && packageSearchDependency.remoteInfo?.name != packageSearchDependency.identifier) {
            append(colored(StringUtils.normalizeSpace(packageSearchDependency.remoteInfo?.name), textColor))
            append(" ")
            append(colored(packageSearchDependency.identifier, textColor2))
        } else {
            append(colored(packageSearchDependency.identifier, textColor))
        }
    }

    private fun applyColors(isSelected: Boolean, list: JList<out PackagesSmartItem>): (JComponent) -> Unit {
        val itemBackground = if (isSelected) list.selectionBackground else list.background
        val itemForeground = if (isSelected) list.selectionForeground else list.foreground

        return {
            it.background = itemBackground
            it.foreground = itemForeground
        }
    }

    @Suppress("LongParameterList")
    private fun buildPanel(
            packageSearchDependency: LicenseDetectorDependency,
            applyColors: (JComponent) -> Unit,
            iconLabel: JLabel,
            idMessage: String,
            licenseName: String
    ): JPanel = JPanel(BorderLayout()).apply {
        @Suppress("MagicNumber") // Gotta love Swing APIs
        if (packageSearchDependency.identifier.isNotBlank()) {
            applyColors(this)
            border = JBEmptyBorder(0, 0, 0, 18)

            add(JPanel().apply {
                applyColors(this)
                layout = BoxLayout(this, BoxLayout.Y_AXIS)
                border = JBEmptyBorder(2, 8, 2, 4)

                add(iconLabel)
            }, BorderLayout.WEST)

            add(JPanel().apply {
                applyColors(this)
                layout = BoxLayout(this, BoxLayout.Y_AXIS)

                add(JLabel("<html>$idMessage</html>"))
            }, BorderLayout.CENTER)

            add(JPanel().apply {
                applyColors(this)
                layout = BoxLayout(this, BoxLayout.X_AXIS)

                add(JLabel("<html>$licenseName</html>"))
            }, BorderLayout.EAST)
        }
    }

    private fun colored(text: String?, color: RiderColor) = "<font color=${color.toHtml()}>${text ?: ""}</font>"
}
