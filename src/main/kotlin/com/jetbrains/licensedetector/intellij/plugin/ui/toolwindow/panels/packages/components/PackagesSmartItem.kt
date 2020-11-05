package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.packages.components

import com.intellij.ide.CopyProvider
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.HyperlinkLabel
import com.intellij.util.ui.JBEmptyBorder
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderUI
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorDependency
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.datatransfer.StringSelection
import javax.swing.JLabel
import javax.swing.JPanel

sealed class PackagesSmartItem {

    object Fake : PackagesSmartItem() {

        val panel = JPanel(BorderLayout()).apply {
            background = RiderUI.MAIN_BG_COLOR
        }
    }

    class Package(val meta: LicenseDetectorDependency) : PackagesSmartItem(), DataProvider, CopyProvider {

        override fun getData(dataId: String): Any? = when {
            PlatformDataKeys.COPY_PROVIDER.`is`(dataId) -> this
            else -> null
        }

        override fun performCopy(dataContext: DataContext) {
            CopyPasteManager.getInstance().setContents(StringSelection(getTextForCopy()))
        }

        override fun isCopyVisible(dataContext: DataContext) = true
        override fun isCopyEnabled(dataContext: DataContext) = true

        private fun getTextForCopy(): String = "${meta.groupId}:${meta.artifactId}"
    }

    class Header(defaultTitle: String) : PackagesSmartItem() {

        private val titleLabel = JLabel(defaultTitle).apply {
            foreground = RiderUI.GRAY_COLOR
            border = JBEmptyBorder(0, 0, 0, 8)
        }

        private val progressIcon = JLabel(AnimatedIcon.Default())
                .apply {
                    isVisible = false
                }

        private val headerLinks = mutableListOf<HyperlinkLabel>()

        var visible = true

        val panel
            get() = if (visible) {
                RiderUI.borderPanel(RiderUI.SectionHeaderBackgroundColor) {
                    RiderUI.setHeight(this, RiderUI.SmallHeaderHeight)
                    border = JBEmptyBorder(3, 0, 0, 18)

                    add(RiderUI.flowPanel(RiderUI.SectionHeaderBackgroundColor) {
                        layout = FlowLayout(FlowLayout.LEFT, 6, 0)

                        add(titleLabel)
                        add(progressIcon)
                    }, BorderLayout.WEST)

                    /*if (headerLinks.any()) {
                        val linksPanel = RiderUI.flowPanel(RiderUI.SectionHeaderBackgroundColor) {
                            border = JBEmptyBorder(-3, 0, 0, -8)

                            headerLinks.forEach { add(it) }
                        }

                        add(linksPanel, BorderLayout.EAST)
                    }*/
                }
            } else {
                Fake.panel
            }

        var title: String
            get() = titleLabel.text
            set(value) {
                titleLabel.text = value
            }

        fun setProgressVisibility(value: Boolean) {
            if (value) {
                visible = true // make sure header is visible when showing progress
            }

            // Show/hide progress icon
            progressIcon.isVisible = value
        }
    }
}
