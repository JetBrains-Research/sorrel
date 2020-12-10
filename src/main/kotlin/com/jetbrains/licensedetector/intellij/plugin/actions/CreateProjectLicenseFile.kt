package com.jetbrains.licensedetector.intellij.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.WriteActionAware
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.IncorrectOperationException
import com.jetbrains.licensedetector.intellij.plugin.licenses.NoLicense
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.LicenseDetectorToolWindowFactory.Companion.ToolWindowModelKey

class CreateProjectLicenseFile : AnAction(), WriteActionAware {

    companion object {
        const val LICENSE_FILE_NAME: String = "LICENSE.txt"

        val LICENSE_FILE_NAME_REGEX: Regex = Regex("LICENSE|LICENSE.txt|LICENSE.md|LICENSE.html", RegexOption.IGNORE_CASE)
    }

    private val actionName = "CreateProjectLicenseFile"

    override fun actionPerformed(e: AnActionEvent) {
        val application = ApplicationManager.getApplication()
        val commandProcessor = CommandProcessor.getInstance()

        val project: Project = e.project!!

        val model = project.getUserData(ToolWindowModelKey)!!

        val baseProjectDir = PsiManager.getInstance(project).findDirectory(project.guessProjectDir()!!)!!


        val createProjectLicenseFile = {
            try {
                val licenseFile: PsiFile = WriteAction.compute(
                        ThrowableComputable<PsiFile, IncorrectOperationException> {
                            baseProjectDir.createFile(LICENSE_FILE_NAME)
                        }
                )

                val licenseDocument = PsiDocumentManager.getInstance(project).getDocument(licenseFile)!!
                val compatibleLicenses = project.getUserData(ToolWindowModelKey)!!
                        .licenseManager.mainProjectCompatibleLicenses.value

                //TODO: Mb must be done in full order in licenses. Now the order is partial
                if (compatibleLicenses.any()) {
                    val recommendedLicense = compatibleLicenses[0]
                    application.runWriteAction {
                        licenseDocument.setText(recommendedLicense.fullText)
                    }
                    model.licenseManager.mainProjectLicense.set(recommendedLicense)
                } else {
                    application.runWriteAction {
                        licenseDocument.setText(NoLicense.fullText)
                    }
                    model.licenseManager.mainProjectLicense.set(NoLicense)
                }

                val openFileDescriptor = OpenFileDescriptor(project, licenseFile.virtualFile)
                openFileDescriptor.navigate(true)

            } catch (e: IncorrectOperationException) {
                //TODO: Log this
            }
        }

        commandProcessor.executeCommand(project,
                createProjectLicenseFile, actionName, null)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project

        if (project == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val projectDirVirtualFile = project.guessProjectDir()
        if (projectDirVirtualFile == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val projectDir = PsiManager.getInstance(project).findDirectory(projectDirVirtualFile)
        if (projectDir == null) {
            e.presentation.isEnabled = false
            return
        }

        if (projectDir.files.any { LICENSE_FILE_NAME_REGEX.matches(it.name) }) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        //When model not exist, for example before indexing
        if (project.getUserData(ToolWindowModelKey) == null) {
            e.presentation.isEnabled = false
            return
        }

        e.presentation.isEnabledAndVisible = true
    }
}