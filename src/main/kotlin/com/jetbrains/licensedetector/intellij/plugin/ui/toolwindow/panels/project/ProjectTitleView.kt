package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.project

import com.intellij.ProjectTopics
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.util.ui.JBEmptyBorder
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.licenses.License
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderColor
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderUI
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorToolWindowModel
import net.miginfocom.layout.AC
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font

class ProjectTitleView(val project: Project, val model: LicenseDetectorToolWindowModel) {

    private val projectNameLabel = RiderUI.createBigLabel().apply {
        text = projectTitleName(project)
    }

    private val pathToProjectDirLabel = RiderUI.createLabel().apply {
        foreground = RiderColor(Color.GRAY, Color.GRAY)
        border = JBUI.Borders.empty(4, 0, 0, 0)
        text = project.basePath ?: ""
    }

    private val projectLicenseLabel = RiderUI.createLabel().apply {
        font = UIUtil.getListFont().let { Font(it.family, it.style, (it.size * 1.1).toInt()) }
        border = JBUI.Borders.empty(4, 0, 0, 0)
        text = mainProjectLicenseName(model.mainProjectLicense.value)
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

        model.mainProjectLicense.advise(model.lifetime) {
            projectLicenseLabel.apply {
                text = mainProjectLicenseName(it)
            }
        }
    }

    private fun projectTitleName(project: Project): String = LicenseDetectorBundle.message("licensedetector.ui.toolwindow.tab.project.project.name") + project.name

    private fun mainProjectLicenseName(license: License): String =
            LicenseDetectorBundle.message("licenseDetector.ui.toolwindow.tab.project.project.main.license") + license.name

    private val infoPanel = RiderUI.headerPanel {
        border = JBEmptyBorder(12, 12, 20, 12)
        layout = MigLayout(
                LC().fillX(),
                AC().grow().gap(), // First column grows as much as it needs
                AC().align("top").gap("1") // 4 units gap before second row (package identifier)
                        .gap("12") // 12 units gap before third row (description)
                        .fill()
        )

        add(projectNameLabel, CC().cell(0, 0).alignY("center"))

        add(pathToProjectDirLabel, CC().cell(0, 1, 3, 1))

        add(projectLicenseLabel, CC().cell(0, 2, 3, 1))
    }

    val panel = RiderUI.headerPanel {
        border = JBUI.Borders.empty(0, 12, 0, 12)

        add(infoPanel, BorderLayout.CENTER)
    }
}