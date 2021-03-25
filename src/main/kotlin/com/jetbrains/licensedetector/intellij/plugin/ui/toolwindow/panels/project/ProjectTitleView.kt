package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.project

import com.intellij.ProjectTopics
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.ui.AnimatedIcon
import com.intellij.util.ui.UIUtil
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.export.ExportJsonLicenseDataAction
import com.jetbrains.licensedetector.intellij.plugin.licenses.NoLicense
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderColor
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderUI
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderUI.Companion.createActionToolbar
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseManager
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.RefreshAction
import com.jetbrains.licensedetector.intellij.plugin.ui.updateAndRepaint
import com.jetbrains.licensedetector.intellij.plugin.utils.licenseDetectorModel
import com.jetbrains.rd.util.lifetime.Lifetime
import net.miginfocom.swing.MigLayout
import java.awt.Color
import java.awt.Font
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.JSeparator
import javax.swing.SwingUtilities

class ProjectTitleView(
    private val project: Project,
    private val licenseManager: LicenseManager,
    lifetime: Lifetime
) {
    private val countComponentsWithoutLicensePanel = 7
    private val projectNameLabel = RiderUI.createBigLabel(projectTitleName(project))

    private val pathToProjectDirLabel = JLabel().apply {
        font = UIUtil.getLabelFont()
        foreground = RiderColor(Color.GRAY, Color.GRAY)
        text = project.basePath ?: ""
    }

    private val progressIcon = JLabel(AnimatedIcon.Default())
        .apply {
            isVisible = false
        }

    val projectTitleViewPanel: JPanel = JPanel().apply {
        background = RiderUI.UsualBackgroundColor

        layout = MigLayout(
            "fillx,flowy,insets 0",
            "[left,grow]0[left]0[right]",
            "[]0[top]10[top][top]5[top]15"
        )

        add(projectNameLabel, "cell 0 0")
        add(progressIcon, "cell 1 0,shrink,align center")
        add(createActionToolbar(RefreshAction(), ExportJsonLicenseDataAction()), "cell 2 0,shrink")
        add(pathToProjectDirLabel, "cell 0 1,span,grow")
        add(createMainProjectLicenseTitle(), "cell 0 2")
        add(JSeparator(), "cell 0 3,growx,span")
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
            SwingUtilities.invokeLater {
                val rootModule = licenseManager.rootModule.value
                val rootModuleLicense = it[rootModule]

                if (rootModuleLicense != null) {
                    if (projectTitleViewPanel.componentCount == countComponentsWithoutLicensePanel) {
                        projectTitleViewPanel.remove(projectTitleViewPanel.components.last())
                        projectTitleViewPanel.add(rootModuleLicense.descriptionPanel(), "span")
                    } else {
                        projectTitleViewPanel.add(rootModuleLicense.descriptionPanel(), "span")
                    }
                    projectTitleViewPanel.updateAndRepaint()
                } else {
                    if (projectTitleViewPanel.componentCount == countComponentsWithoutLicensePanel) {
                        projectTitleViewPanel.remove(projectTitleViewPanel.components.last())
                        projectTitleViewPanel.add(NoLicense.descriptionPanel(), "span")
                    } else {
                        projectTitleViewPanel.add(NoLicense.descriptionPanel(), "span")
                    }
                    projectTitleViewPanel.updateAndRepaint()
                }
            }
        }

        licenseManager.rootModule.advise(lifetime) {
            SwingUtilities.invokeLater {
                val rootModuleLicense = licenseManager.modulesLicenses.value[it]
                if (rootModuleLicense != null) {
                    if (projectTitleViewPanel.componentCount == countComponentsWithoutLicensePanel) {
                        projectTitleViewPanel.remove(projectTitleViewPanel.components.last())
                        projectTitleViewPanel.add(rootModuleLicense.descriptionPanel(), "span")
                    } else {
                        projectTitleViewPanel.add(rootModuleLicense.descriptionPanel(), "span")
                    }
                    projectTitleViewPanel.updateAndRepaint()
                } else {
                    if (projectTitleViewPanel.componentCount == countComponentsWithoutLicensePanel) {
                        projectTitleViewPanel.remove(projectTitleViewPanel.components.last())
                        projectTitleViewPanel.add(NoLicense.descriptionPanel(), "span")
                    } else {
                        projectTitleViewPanel.add(NoLicense.descriptionPanel(), "span")
                    }
                    projectTitleViewPanel.updateAndRepaint()
                }
            }
        }

        val licenseDetectorModel = project.licenseDetectorModel()

        licenseDetectorModel.status.advise(licenseDetectorModel.lifetime) {
            progressIcon.isVisible = it.isBusy
            projectTitleViewPanel.updateAndRepaint()
        }
    }

    private fun projectTitleName(project: Project): String =
        LicenseDetectorBundle.message("licensedetector.ui.toolwindow.tab.project.project.name") + project.name

    private fun createMainProjectLicenseTitle(): JLabel = JLabel(
        LicenseDetectorBundle.message("licensedetector.ui.toolwindow.tab.project.project.main.license")
    ).apply {
        font = Font(font.family, Font.BOLD, (font.size * 1.2).toInt())
    }
}