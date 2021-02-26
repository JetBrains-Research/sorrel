package com.jetbrains.licensedetector.intellij.plugin.notification

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.module.ModuleUtilCore.findModuleForFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.jetbrains.licensedetector.intellij.plugin.detection.Detector.licenseFileNamePattern
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.LicenseDetectorToolWindowFactory.Companion.ToolWindowModelKey


class LicenseFileEditorNotificationProvider : EditorNotifications.Provider<EditorNotificationPanel>() {

    companion object {
        private val KEY: Key<EditorNotificationPanel> = Key.create("LicenseFile")
    }

    override fun getKey(): Key<EditorNotificationPanel> = KEY

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor, project: Project): EditorNotificationPanel? {
        val fileName = file.name
        if (!licenseFileNamePattern.matches(fileName)) {
            return null
        }

        findModuleForFile(file, project) ?: return null

        val model = project.getUserData(ToolWindowModelKey) ?: return null

        return LicenseFileEditorNotificationPanel(model, project, file)
    }
}