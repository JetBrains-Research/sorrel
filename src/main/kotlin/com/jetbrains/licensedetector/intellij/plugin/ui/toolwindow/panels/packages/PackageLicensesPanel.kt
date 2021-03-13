package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.packages

import com.intellij.icons.AllIcons
import com.intellij.ide.ui.LafManagerListener
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.wm.IdeFocusManager
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.ui.ComponentActionWrapper
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderUI
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.ToolWindowModel
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.PanelBase
import com.jetbrains.licensedetector.intellij.plugin.ui.updateAndRepaint
import net.miginfocom.swing.MigLayout
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.*
import javax.swing.event.DocumentEvent

class PackageLicensesPanel(
    val viewModel: ToolWindowModel
) : PanelBase(LicenseDetectorBundle.message("licensedetector.ui.toolwindow.tab.packages.title")) {

    private val smartList = PackagesSmartList(viewModel)

    val searchTextField = PackagesSmartSearchField(viewModel)
            .apply {
                goToList = {
                    if (smartList.hasPackageItems) {
                        smartList.selectedIndex = smartList.firstPackageIndex
                        IdeFocusManager.getInstance(viewModel.project).requestFocus(smartList, false)
                        true
                    } else {
                        false
                    }
                }
            }

    private val packagesPanel = RiderUI.borderPanel {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }

    private val moduleContextComboBox = ModuleContextComboBox(viewModel)

    private val refreshAction =
            object : AnAction(
                    LicenseDetectorBundle.message("licensedetector.ui.toolwindow.actions.reload.text"),
                    LicenseDetectorBundle.message("licensedetector.ui.toolwindow.actions.reload.description"),
                    AllIcons.Actions.Refresh
            ) {
                override fun actionPerformed(e: AnActionEvent) {
                    viewModel.requestRefreshContext.fire(Unit)
                }
            }


    private fun updateLaf() {
        @Suppress("MagicNumber") // Gotta love Swing APIs
        with(searchTextField) {
            textEditor.putClientProperty("JTextField.Search.Gap", JBUI.scale(6))
            textEditor.putClientProperty("JTextField.Search.GapEmptyText", JBUI.scale(-1))
            textEditor.border = JBUI.Borders.empty(0, 6, 0, 0)
            textEditor.isOpaque = true
            textEditor.background = RiderUI.HeaderBackgroundColor
        }
    }

    private fun createRefreshActionGroup() = DefaultActionGroup(refreshAction)

    private val refreshToolbar = ActionManager.getInstance().createActionToolbar(
            "",
            createRefreshActionGroup(),
            true
    ).apply {
        component.background = RiderUI.HeaderBackgroundColor
        component.border = BorderFactory.createMatteBorder(0, JBUI.scale(1), 0, 0,
                JBUI.CurrentTheme.CustomFrameDecorations.paneBackground())
    }

    private fun createModuleSelectionActionGroup() = DefaultActionGroup().apply {
        add(ComponentActionWrapper { moduleContextComboBox })
    }

    private val moduleSelectionToolbar = ActionManager.getInstance().createActionToolbar(
            "",
            createModuleSelectionActionGroup(),
            true
    ).apply {
        component.background = RiderUI.HeaderBackgroundColor
        component.border = BorderFactory.createMatteBorder(0, JBUI.scale(1), 0, 0,
                JBUI.CurrentTheme.CustomFrameDecorations.paneBackground())
    }

    private val headerPanel = RiderUI.headerPanel {
        RiderUI.setHeight(this, RiderUI.MediumHeaderHeight)

        border = BorderFactory.createEmptyBorder()

        addToCenter(object : JPanel() {
            init {
                //TODO: Fix right insets on last component
                layout = MigLayout("ins 0 0 0 0, fill", "[left, fill, grow][][right]", "center")
                add(searchTextField)
                add(moduleSelectionToolbar.component)
                add(refreshToolbar.component)
            }

            override fun getBackground() = RiderUI.UsualBackgroundColor
        })
    }

    private val scrollPane = JBScrollPane(
            packagesPanel.apply {
                add(createListPanel(smartList))
                add(Box.createVerticalGlue())
            },
            JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER
    ).apply {
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
        viewModel.searchTerm.set("")

        viewModel.isBusy.advise(viewModel.lifetime) {
            searchTextField.isEnabled = !it
        }

        smartList.transferFocusUp = {
            IdeFocusManager.getInstance(viewModel.project).requestFocus(searchTextField, false)
        }

        searchTextField.addDocumentListener(object : DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                ApplicationManager.getApplication().invokeLater {
                    viewModel.searchTerm.set(searchTextField.text)
                }
            }
        })
        viewModel.searchTerm.advise(viewModel.lifetime) { searchTerm ->
            if (searchTextField.text != searchTerm) {
                searchTextField.text = searchTerm
            }
        }

        viewModel.searchResultsUpdated.advise(viewModel.lifetime) {
            smartList.updateAllPackages(it.values.toList())
            packagesPanel.updateAndRepaint()
        }

        smartList.addPackageSelectionListener {
            viewModel.selectedPackage.set(it.identifier)
        }

        viewModel.isSearching.advise(viewModel.lifetime) {
            smartList.installedHeader.setProgressVisibility(it)
            smartList.updateAndRepaint()
            packagesPanel.updateAndRepaint()
        }

        // LaF
        updateLaf()
        ApplicationManager.getApplication().messageBus.connect().subscribe(
                LafManagerListener.TOPIC, LafManagerListener { updateLaf() }
        )

        //Paint packages after build ui
        //viewModel.refreshFoundPackages()
    }

    override fun build() = RiderUI.boxPanel {
        add(headerPanel)
        add(scrollPane)

        @Suppress("MagicNumber") // Swing APIs are <3
        minimumSize = Dimension(JBUI.scale(200), minimumSize.height)
    }
}
