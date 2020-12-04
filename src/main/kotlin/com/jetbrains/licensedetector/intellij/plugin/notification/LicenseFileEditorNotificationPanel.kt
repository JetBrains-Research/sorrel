package com.jetbrains.licensedetector.intellij.plugin.notification

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationPanel
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.licenses.ALL_SUPPORTED_LICENSE
import com.jetbrains.licensedetector.intellij.plugin.licenses.SupportedLicense
import com.jetbrains.licensedetector.intellij.plugin.licenses.getLicenseOnFullTextOrNull
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderUI
import com.jetbrains.licensedetector.intellij.plugin.ui.RiderUI.Companion.comboBox
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorToolWindowModel
import com.jetbrains.licensedetector.intellij.plugin.ui.updateAndRepaint

class LicenseFileEditorNotificationPanel(
        val model: LicenseDetectorToolWindowModel,
        val project: Project,
        val licenseFile: VirtualFile
) : EditorNotificationPanel() {

    private val actionName = "Change project license file"

    init {
        setText(LicenseDetectorBundle.message("licensedetector.ui.editor.notification.license.file.title"))
        myBackgroundColor = RiderUI.HeaderBackgroundColor

        val comboBoxCompatibleLicenses = createComboBoxWithLicenses()

        myLinksPanel.add(comboBoxCompatibleLicenses)

        val licenseDocument: Document = ReadAction.compute<Document, Throwable> {
            FileDocumentManager.getInstance().getDocument(licenseFile)!!
        }

        createUpdateLicenseFileTextActionLabel(comboBoxCompatibleLicenses, licenseDocument)
        addUpdateOnLicenseFileText(comboBoxCompatibleLicenses, licenseDocument)

        model.projectLicensesCompatibleWithPackageLicenses.advise(model.lifetime) {
            updateAndRepaint()
            comboBoxCompatibleLicenses.updateAndRepaint()
        }
    }

    private fun createComboBoxWithLicenses(): ComboBox<SupportedLicense> {
        val mainProjectLicense = model.mainProjectLicense.value
        val comboBox = comboBox(ALL_SUPPORTED_LICENSE)
        comboBox.isSwingPopup = false
        comboBox.renderer = LicenseListCellRenderer(model)
        comboBox.selectedItem = mainProjectLicense
        addUpdateProjectLicenseFileActions(comboBox)
        return comboBox
    }

    private fun createUpdateLicenseFileTextActionLabel(
            comboBox: ComboBox<SupportedLicense>,
            licenseDocument: Document) {
        val application = ApplicationManager.getApplication()
        val commandProcessor = CommandProcessor.getInstance()

        createActionLabel(LicenseDetectorBundle.message("licensedetector.ui.editor.notification.license.file.action.updateLicenseFileText")) {
            val selectedLicense = (comboBox.selectedItem as SupportedLicense)

            commandProcessor.executeCommand(project, {
                application.runWriteAction {
                    licenseDocument.setText(selectedLicense.fullText)
                }
            }, actionName, null)
        }
    }

    private fun addUpdateProjectLicenseFileActions(comboBox: ComboBox<SupportedLicense>) {
        comboBox.addActionListener {
            val selectedLicense = (comboBox.selectedItem as SupportedLicense)
            model.mainProjectLicense.set(selectedLicense)
        }
    }

    private fun addUpdateOnLicenseFileText(comboBox: ComboBox<SupportedLicense>, licenseDocument: Document) {
        licenseDocument.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val licenseDocumentText = licenseDocument.text
                // TODO: Add using ml license text resolver
                val license = getLicenseOnFullTextOrNull(licenseDocumentText) ?: return
                comboBox.selectedItem = license
            }
        })
    }
}