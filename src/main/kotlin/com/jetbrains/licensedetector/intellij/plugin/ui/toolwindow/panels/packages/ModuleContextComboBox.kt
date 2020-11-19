package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.panels.packages

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.jetbrains.licensedetector.intellij.plugin.module.ProjectModule
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorToolWindowModel
import com.jetbrains.packagesearch.intellij.plugin.api.PackageSearchBundle
import javax.swing.JLabel

class ModuleContextComboBox(viewModel: LicenseDetectorToolWindowModel) : ContextComboBoxBase(viewModel) {

    override fun createNameLabel() = JLabel("")
    override fun createValueLabel() = object : JLabel() {
        override fun getIcon() = AllIcons.General.ProjectStructure

        override fun getText() = viewModel.selectedProjectModule.value?.name
                ?: PackageSearchBundle.message("packagesearch.ui.toolwindow.allModules")
    }

    override fun createActionGroup(): ActionGroup {
        return DefaultActionGroup(
                createSelectProjectAction(),
                DefaultActionGroup(createSelectModuleActions())
        )
    }

    private fun createSelectProjectAction() = createSelectAction(null, PackageSearchBundle.message("packagesearch.ui.toolwindow.allModules"))

    private fun createSelectModuleActions(): List<AnAction> =
            viewModel.projectModules.value
                    .sortedBy { it.getFullName() }
                    .map {
                        createSelectAction(it, it.getFullName())
                    }

    private fun createSelectAction(projectModule: ProjectModule?, title: String) =
            object : AnAction(title, title, AllIcons.General.ProjectStructure) {
                override fun actionPerformed(e: AnActionEvent) {
                    viewModel.selectedProjectModule.set(projectModule)
                    updateLabel()
                }
            }
}
