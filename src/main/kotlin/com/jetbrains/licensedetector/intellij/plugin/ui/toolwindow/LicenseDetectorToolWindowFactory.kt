package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.DumbUnawareHider
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorToolWindowModel
import javax.swing.JLabel

class LicenseDetectorToolWindowFactory : ToolWindowFactory, DumbAware {

    companion object {

        //Must be equal to "id" in ToolWindow EP in plugin.xml
        private val ToolWindowId = LicenseDetectorBundle.message("licensedetector.ui.toolwindow.title")

        val ToolWindowModelKey = Key.create<LicenseDetectorToolWindowModel>("LicenseDetector.Management.Model")

        //May be needed for tab cross-interaction
        /*
        private fun getToolWindow(project: Project) = ToolWindowManager.getInstance(project).getToolWindow(ToolWindowId)

        fun activateToolWindow(project: Project) {
            getToolWindow(project)?.activate {}
        }

        fun activateToolWindow(project: Project, action: () -> Unit) {
            getToolWindow(project)?.activate(action, true, true)
        }

        fun toggleToolWindow(project: Project) {
            getToolWindow(project)?.let {
                if (it.isVisible) {
                    it.hide { }
                } else {
                    it.activate(null, true, true)
                }
            }
        }
         */
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // On first load, show "unavailable while indices are built"
        toolWindow.contentManager.addContent(
                ContentFactory.SERVICE.getInstance().createContent(
                        DumbUnawareHider(JLabel()).apply { setContentVisible(false) },
                        LicenseDetectorBundle.message("licensedetector.ui.toolwindow.tab.packages.title"), false
                ).apply {
                    isCloseable = false
                })

        // Once indices have been built once, show tool window forever
        DumbService.getInstance(project).runWhenSmart {
            ServiceManager.getService(project, LicenseDetectorToolWindowAvailabilityService::class.java).initialize(toolWindow)
        }
    }
}