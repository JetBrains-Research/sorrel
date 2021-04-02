package com.jetbrains.licensedetector.intellij.plugin.toolwindow.panels.packages

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.model.ProjectModule
import com.jetbrains.licensedetector.intellij.plugin.model.ToolWindowModel
import com.jetbrains.licensedetector.intellij.plugin.utils.licenseDetectorModel
import javax.swing.JLabel

internal class ModuleContextComboBox(val project: Project) : ContextComboBoxBase() {
    private val model: ToolWindowModel = project.licenseDetectorModel()

    override fun createNameLabel() = JLabel("")
    override fun createValueLabel() = object : JLabel() {
        override fun getIcon() = AllIcons.General.ProjectStructure

        override fun getText(): String {
            // I completely don't know why model sometimes is null
            return if (model != null) {
                model.selectedProjectModule.value?.name
                    ?: LicenseDetectorBundle.message("licensedetector.ui.toolwindow.allModules")
            } else {
                LicenseDetectorBundle.message("licensedetector.ui.toolwindow.allModules")
            }
        }
    }

    override fun createActionGroup(): ActionGroup {
        return DefaultActionGroup(
            createSelectProjectAction(),
            DefaultActionGroup(createSelectModuleActions())
        )
    }

    private fun createSelectProjectAction() =
        createSelectAction(null, LicenseDetectorBundle.message("licensedetector.ui.toolwindow.allModules"))

    private fun createSelectModuleActions(): List<AnAction> =
        model.projectModules.value
            .sortedBy { it.name }
            .map {
                createSelectAction(it, it.name)
            }

    private fun createSelectAction(projectModule: ProjectModule?, title: String) =
        object : AnAction(title, title, AllIcons.General.ProjectStructure) {
            override fun actionPerformed(e: AnActionEvent) {
                model.selectedProjectModule.set(projectModule)
                updateLabel()
            }
        }
}
