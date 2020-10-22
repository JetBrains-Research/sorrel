package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.left

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.ui.AutoScrollToSourceHandler
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderUI
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorToolWindowModel
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.PackageSearchPanelBase
import com.jetbrains.licensedetector.intellij.plugin.ui.updateAndRepaint
import net.miginfocom.swing.MigLayout
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*

class PackagesSmartPanel(
        val viewModel: LicenseDetectorToolWindowModel,
        autoScrollToSourceHandler: AutoScrollToSourceHandler
) : PackageSearchPanelBase(LicenseDetectorBundle.message("licensedetector.ui.toolwindow.tab.packages.title")) {

    private val smartList = PackagesSmartList(viewModel)

    private val packagesPanel = RiderUI.borderPanel {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    //TODO: mb needed
    //private val moduleContextComboBox = ModuleContextComboBox(viewModel)

    private fun createActionGroup() = DefaultActionGroup().apply {
        //add(ComponentActionWrapper { moduleContextComboBox })
    }

    private val mainToolbar = ActionManager.getInstance().createActionToolbar("", createActionGroup(), true).apply {
        component.background = RiderUI.HeaderBackgroundColor
        component.border = BorderFactory.createMatteBorder(0, JBUI.scale(1), 0, 0, JBUI.CurrentTheme.CustomFrameDecorations.paneBackground())
    }

    private val headerPanel = RiderUI.headerPanel {
        RiderUI.setHeight(this, RiderUI.MediumHeaderHeight)

        border = BorderFactory.createEmptyBorder()

        addToCenter(object : JPanel() {
            init {
                layout = MigLayout("ins 0, fill", "[left, fill, grow][right]", "center")
                add(mainToolbar.component)
            }

            override fun getBackground() = RiderUI.UsualBackgroundColor
        })
    }

    private val scrollPane = JBScrollPane(packagesPanel.apply {
        add(createListPanel(smartList))
        add(Box.createVerticalGlue())
    }, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER).apply {
        this.border = BorderFactory.createMatteBorder(JBUI.scale(1), 0, 0, 0, JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground())
        this.verticalScrollBar.unitIncrement = 16

        UIUtil.putClientProperty(verticalScrollBar, JBScrollPane.IGNORE_SCROLLBAR_IN_INSETS, true)
    }

    private fun createListPanel(list: PackagesSmartList) = RiderUI.borderPanel {
        minimumSize = Dimension(1, 1)
        maximumSize = Dimension(Int.MAX_VALUE, maximumSize.height)
        add(list, BorderLayout.NORTH)
        RiderUI.updateParentHeight(list)
    }

    init {

        viewModel.searchResultsUpdated.advise(viewModel.lifetime) {
            smartList.updateAllPackages(it.values.toList())
            packagesPanel.updateAndRepaint()
        }

        viewModel.isFetchingSuggestions.advise(viewModel.lifetime) {
            smartList.installedHeader.setProgressVisibility(it)
            smartList.updateAndRepaint()
            packagesPanel.updateAndRepaint()
        }
    }

    override fun build() = RiderUI.boxPanel {
        add(headerPanel)
        add(scrollPane)

        @Suppress("MagicNumber") // Swing APIs are <3
        minimumSize = Dimension(JBUI.scale(200), minimumSize.height)
    }
}
