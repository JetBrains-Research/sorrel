package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow

import com.intellij.ProjectTopics
import com.intellij.openapi.Disposable
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.DumbUnawareHider
import com.intellij.openapi.project.ModuleListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.createLifetime
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorToolWindowModel
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.PanelBase
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.packages.PackageLicensesPanel
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.project.ProjectLicensePanel
import javax.swing.JComponent
import javax.swing.JLabel

class LicenseDetectorToolWindowAvailabilityService(val project: Project) : Disposable {

    private var toolWindow: ToolWindow? = null
    private var toolWindowContentsCreated = false
    private var wasAvailable = false

    fun initialize(toolWindow: ToolWindow) {
        this.toolWindow = toolWindow

        setAvailabilityBasedOnProjectModules(project)
        startMonitoring()
    }

    private fun startMonitoring() {
        project.messageBus.connect().subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                setAvailabilityBasedOnProjectModules(project)
            }
        })

        project.messageBus.connect().subscribe(ProjectTopics.MODULES, object : ModuleListener {
            override fun moduleAdded(p: Project, module: Module) {
                setAvailabilityBasedOnProjectModules(project)
            }

            override fun moduleRemoved(p: Project, module: Module) {
                setAvailabilityBasedOnProjectModules(project)
            }
        })
    }

    private fun setAvailabilityBasedOnProjectModules(project: Project) {
        val isAvailable = ModuleManager.getInstance(project).modules.isNotEmpty()
        toolWindow?.let {
            if (wasAvailable != isAvailable || !toolWindowContentsCreated) {
                createToolWindowContents(it, isAvailable)
            }
        }
    }

    private fun createToolWindowContents(toolWindow: ToolWindow, isAvailable: Boolean) {
        toolWindowContentsCreated = true
        wasAvailable = isAvailable

        toolWindow.title = LicenseDetectorBundle.message("licensedetector.ui.toolwindow.title")

        val contentManager = toolWindow.contentManager

        contentManager.removeAllContents(false)

        if (!isAvailable) {
            contentManager.addContent(ContentFactory.SERVICE.getInstance().createContent(
                    DumbUnawareHider(JLabel()).apply {
                        setContentVisible(false)
                        emptyText.text = LicenseDetectorBundle.message("licensedetector.ui.toolwindow.tab.noModules")
                    },
                    LicenseDetectorBundle.message("licensedetector.ui.toolwindow.tab.packages.title"), false
            ).apply {
                isCloseable = false
            })

            return
        }

        val model = LicenseDetectorToolWindowModel(project, createLifetime())
        project.putUserData(LicenseDetectorToolWindowFactory.ToolWindowModelKey, model)

        addPanel(contentManager, PackageLicensesPanel(model))
        addPanel(contentManager, ProjectLicensePanel(project, model))
    }

    private fun addPanel(contentManager: ContentManager, panel: PanelBase) {
        contentManager.addTab(panel.title, panel.content, panel.toolbar)
    }

    private fun ContentManager.addTab(title: String, content: JComponent, toolbar: JComponent?) {
        addContent(ContentFactory.SERVICE.getInstance().createContent(null, title, false).apply {
            component = SimpleToolWindowPanel(false).setProvideQuickActions(true).apply {
                setContent(content)
                toolbar?.let { setToolbar(it) }

                isCloseable = false
            }
        })
    }

    override fun dispose() {
        // Nothing to dispose
    }
}