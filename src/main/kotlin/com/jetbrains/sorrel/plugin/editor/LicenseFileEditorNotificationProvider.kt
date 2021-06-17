package com.jetbrains.sorrel.plugin.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.module.ModuleUtilCore.findModuleForFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.intellij.ui.EditorNotifications
import com.jetbrains.sorrel.plugin.detection.DetectorManager.licenseFileNamePattern
import com.jetbrains.sorrel.plugin.utils.licenseDetectorModel


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

        val module = findModuleForFile(file, project) ?: return null
        project.licenseDetectorModel().projectModules.value.find { it.nativeModule == module } ?: return null

        findModuleForFile(file, project) ?: return null

        return LicenseFileEditorNotificationPanel(project, file)
    }
}