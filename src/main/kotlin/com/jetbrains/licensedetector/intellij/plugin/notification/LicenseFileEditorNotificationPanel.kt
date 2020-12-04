package com.jetbrains.licensedetector.intellij.plugin.notification

import com.intellij.diff.DiffContentFactory
import com.intellij.diff.DiffDialogHints
import com.intellij.diff.DiffManager
import com.intellij.diff.actions.impl.MutableDiffRequestChain
import com.intellij.diff.contents.DocumentContent
import com.intellij.diff.util.DiffUserDataKeys
import com.intellij.diff.util.Side
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.Pair
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

        createShowDiffLicenseFileActionLabel(comboBoxCompatibleLicenses, licenseFile)

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

    private fun createShowDiffLicenseFileActionLabel(
            comboBox: ComboBox<SupportedLicense>,
            licenseFile: VirtualFile) {
        createActionLabel(LicenseDetectorBundle.message("licensedetector.ui.editor.notification.license.file.action.showDiffLicenseFile.label")) {
            val diffContentFactory = DiffContentFactory.getInstance()

            val selectedLicense = comboBox.selectedItem as SupportedLicense
            val selectedLicenseDocument = EditorFactory.getInstance().createDocument(selectedLicense.fullText)
            selectedLicenseDocument.setReadOnly(true)
            val referenceLicenseContent = diffContentFactory.create(
                    project,
                    selectedLicenseDocument,
                    PlainTextFileType.INSTANCE
            )
            val currentLicenseFileContent = diffContentFactory.create(project, licenseFile)

            val chain = MutableDiffRequestChain(currentLicenseFileContent, referenceLicenseContent)

            if (currentLicenseFileContent is DocumentContent) {
                val editors = EditorFactory.getInstance().getEditors(currentLicenseFileContent.document)
                if (editors.isNotEmpty()) {
                    val currentLine = editors[0].caretModel.logicalPosition.line
                    chain.putRequestUserData(DiffUserDataKeys.SCROLL_TO_LINE, Pair.create(Side.LEFT, currentLine))
                }
            }

            chain.putRequestUserData(DiffUserDataKeys.DO_NOT_IGNORE_WHITESPACES, true)

            chain.windowTitle = licenseFile.name +
                    LicenseDetectorBundle.message("licensedetector.ui.editor.notification.license.file.action.showDiffLicenseFile.vs") +
                    LicenseDetectorBundle.message("licensedetector.ui.editor.notification.license.file.action.showDiffLicenseFile.referenceLicenseFile")
            chain.title1 = licenseFile.name
            chain.title2 = LicenseDetectorBundle.message("licensedetector.ui.editor.notification.license.file.action.showDiffLicenseFile.referenceLicenseFile")

            DiffManager.getInstance().showDiff(project, chain, DiffDialogHints.DEFAULT)
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