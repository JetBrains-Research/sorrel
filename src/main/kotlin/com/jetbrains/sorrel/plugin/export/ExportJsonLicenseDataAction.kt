package com.jetbrains.sorrel.plugin.export

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task.Backgroundable
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.jetbrains.sorrel.plugin.export.model.ExportLicenseData
import com.jetbrains.sorrel.plugin.export.notification.ExportLicenseDataNotification
import com.jetbrains.sorrel.plugin.utils.licenseDetectorModel

class ExportJsonLicenseDataAction : AnAction(
    com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.toolwindow.actions.export.text"),
    com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.toolwindow.actions.export.description"),
    com.jetbrains.sorrel.plugin.SorrelPluginIcons.Export
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project!!
        val saveDialog = FileChooserFactory.getInstance().createSaveFileDialog(
            FileSaverDescriptor(
                com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.toolwindow.actions.export.saver.title"),
                com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.toolwindow.actions.export.saver.description"),
                "json"
            ),
            project
        )
        val target = saveDialog.save(null, project.name + "_license_data.json")
        if (target != null) {
            val task: Backgroundable = object : Backgroundable(project, "Export license data", false) {
                override fun run(indicator: ProgressIndicator) {
                    target.file.writeText(
                        ExportLicenseData.createCollectedLicenseDataJson(project)
                    )
                }

                override fun onSuccess() {
                    val successNotification = ExportLicenseDataNotification(
                        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.toolwindow.actions.export.notification.success.title"),
                        NotificationType.INFORMATION
                    )
                    successNotification.addAction(ShowLicenseDataAction(target.file))
                    successNotification.notify(project)
                }

                override fun onError(error: Exception) {
                    val successNotification = ExportLicenseDataNotification(
                        com.jetbrains.sorrel.plugin.SorrelBundle.message("sorrel.ui.toolwindow.actions.export.notification.error.title"),
                        NotificationType.WARNING
                    )
                    successNotification.notify(project)
                }
            }
            ProgressManager.getInstance()
                .runProcessWithProgressAsynchronously(task, BackgroundableProcessIndicator(task))
        }
    }


    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabled = project != null && !project.licenseDetectorModel().status.value.isBusy
    }
}