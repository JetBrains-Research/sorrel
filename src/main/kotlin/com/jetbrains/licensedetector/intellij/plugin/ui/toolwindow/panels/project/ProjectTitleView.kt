package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.project

import com.intellij.ProjectTopics
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.util.ui.UIUtil
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderColor
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderUI
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseManager
import com.jetbrains.licensedetector.intellij.plugin.ui.updateAndRepaint
import com.jetbrains.rd.util.lifetime.Lifetime
import net.miginfocom.layout.CC
import net.miginfocom.swing.MigLayout
import java.awt.Color
import java.awt.Font
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSeparator

class ProjectTitleView(
        private val project: Project,
        private val licenseManager: LicenseManager,
        lifetime: Lifetime
) {
    private val projectNameLabel = RiderUI.createBigLabel(projectTitleName(project))

    private val pathToProjectDirLabel = JLabel().apply {
        font = UIUtil.getLabelFont()
        foreground = RiderColor(Color.GRAY, Color.GRAY)
        text = project.basePath ?: ""
    }

    val projectTitleViewPanel: JPanel = JPanel().apply {
        background = RiderUI.UsualBackgroundColor

        layout = MigLayout(
            "fillx,flowy,insets 0",
            "[left,grow]",
            "[top]0[top]10[top][top]5[top]15"
        )

        add(projectNameLabel)
        add(pathToProjectDirLabel)
        add(createMainProjectLicenseTitle())
        add(JSeparator(), CC().growX())
    }

    init {
        project.messageBus.connect().subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            //TODO: Fix this bug
            // When the project name is renamed, it should be updated in the user interface.
            // But in the event instance, we have the old project name and we are not updating it correctly.
            override fun rootsChanged(event: ModuleRootEvent) {
                projectNameLabel.text = projectTitleName(project)
            }
        })

        //TODO: Mb need to update pathToProjectDirLabel when project moved

        licenseManager.modulesLicenses.advise(lifetime) {
            val rootModule = licenseManager.rootModule.value
            val rootModuleLicense = it[rootModule]

            if (rootModuleLicense != null) {
                if (projectTitleViewPanel.componentCount == 5) {
                    projectTitleViewPanel.remove(projectTitleViewPanel.components.last())
                    projectTitleViewPanel.add(rootModuleLicense.descriptionPanel)
                } else {
                    projectTitleViewPanel.add(rootModuleLicense.descriptionPanel)
                }
                projectTitleViewPanel.updateAndRepaint()
            }
        }

        licenseManager.rootModule.advise(lifetime) {
            val rootModuleLicense = licenseManager.modulesLicenses.value[it]

            if (rootModuleLicense != null) {
                if (projectTitleViewPanel.componentCount == 5) {
                    projectTitleViewPanel.remove(projectTitleViewPanel.components.last())
                    projectTitleViewPanel.add(rootModuleLicense.descriptionPanel)
                } else {
                    projectTitleViewPanel.add(rootModuleLicense.descriptionPanel)
                }
                projectTitleViewPanel.updateAndRepaint()
            }/* else {
                if (projectTitleViewPanel.componentCount == 5) {
                    projectTitleViewPanel.remove(projectTitleViewPanel.components.last())
                    projectTitleViewPanel.add(NoLicense.descriptionPanel)
                } else {
                    projectTitleViewPanel.add(NoLicense.descriptionPanel)
                }
                projectTitleViewPanel.updateAndRepaint()
            }*/
        }
    }

    private fun projectTitleName(project: Project): String = LicenseDetectorBundle.message("licensedetector.ui.toolwindow.tab.project.project.name") + project.name

    private fun createMainProjectLicenseTitle(): JLabel = JLabel(
            LicenseDetectorBundle.message("licensedetector.ui.toolwindow.tab.project.project.main.license")
    ).apply {
        font = Font(font.family, Font.BOLD, (font.size * 1.2).toInt())
    }
}