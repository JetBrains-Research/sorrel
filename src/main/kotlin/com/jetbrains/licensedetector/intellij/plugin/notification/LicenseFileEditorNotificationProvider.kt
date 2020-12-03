package com.jetbrains.licensedetector.intellij.plugin.notification

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.jetbrains.licensedetector.intellij.plugin.actions.CreateProjectLicenseFile.Companion.LICENSE_FILE_NAME
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.LicenseDetectorToolWindowFactory.Companion.ToolWindowModelKey


class LicenseFileEditorNotificationProvider : EditorNotifications.Provider<EditorNotificationPanel>() {

    companion object {
        private val KEY: Key<EditorNotificationPanel> = Key.create("LicenseFile")
    }

    override fun getKey(): Key<EditorNotificationPanel> = KEY

    override fun createNotificationPanel(file: VirtualFile, fileEditor: FileEditor, project: Project): EditorNotificationPanel? {
        val fileName = file.name
        if (fileName != LICENSE_FILE_NAME) {
            return null
        }

        val fileParentDir = file.parent
        if (fileParentDir != project.guessProjectDir()) {
            return null
        }

        val model = project.getUserData(ToolWindowModelKey) ?: return null

        return LicenseFileEditorNotificationPanel(model, project, file)
    }
}