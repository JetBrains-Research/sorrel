package com.jetbrains.licensedetector.intellij.plugin.notification

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.licenses.SupportedLicense
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorToolWindowModel

class LicenseFileEditorNotificationPanel(
        val model: LicenseDetectorToolWindowModel,
        val project: Project,
        val licenseFile: VirtualFile
) : EditorNotificationPanel() {

    private val actionName = "Change project license file"

    init {
        setText(LicenseDetectorBundle.message("licensedetector.ui.editor.notification.license.file.title"))

        val comboBoxCompatibleLicenses = createComboBoxWithLicenses()

        myLinksPanel.add(comboBoxCompatibleLicenses)
    }

    private fun createComboBoxWithLicenses(): ComboBox<SupportedLicense> {
        val compatibleLicenses = model.projectLicensesCompatibleWithPackageLicenses.value
        val mainProjectLicense = model.mainProjectLicense.value
        val comboBox = ComboBox(model.projectLicensesCompatibleWithPackageLicenses.value.toTypedArray())
        comboBox.renderer = LicenseListCellRenderer()
        comboBox.selectedItem = model.mainProjectLicense.value
        addUpdateProjectLicenseFileActions(comboBox)
        return comboBox
    }

    private fun addUpdateProjectLicenseFileActions(comboBox: ComboBox<SupportedLicense>) {
        val application = ApplicationManager.getApplication()
        val commandProcessor = CommandProcessor.getInstance()

        comboBox.addActionListener {

            val selectedLicense = (comboBox.selectedItem as SupportedLicense)

            val licenseDocument: Document = ReadAction.compute<Document, Throwable> {
                FileDocumentManager.getInstance().getDocument(licenseFile)!!
            }

            //Launching without CommandProcessor because when the action is canceled,
            // there is no way to revert the value in the combobox

            application.runWriteAction {
                licenseDocument.setText(selectedLicense.fullText)
            }
            model.mainProjectLicense.set(selectedLicense)
        }
    }
}