package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.packages

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.module.ProjectModule
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.ToolWindowModel
import javax.swing.JLabel

class ModuleContextComboBox(val model: ToolWindowModel) : ContextComboBoxBase(model) {

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
            .sortedBy { it.getFullName() }
            .map {
                createSelectAction(it, it.getFullName())
            }

    private fun createSelectAction(projectModule: ProjectModule?, title: String) =
        object : AnAction(title, title, AllIcons.General.ProjectStructure) {
            override fun actionPerformed(e: AnActionEvent) {
                model.selectedProjectModule.set(projectModule)
                updateLabel()
            }
        }
}
