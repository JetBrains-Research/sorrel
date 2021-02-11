package com.jetbrains.licensedetector.intellij.plugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.WriteActionAware
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.module.ModuleUtilCore.findModuleForFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessModuleDir
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.util.IncorrectOperationException
import com.jetbrains.licensedetector.intellij.plugin.licenses.NoLicense
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.LicenseDetectorToolWindowFactory.Companion.ToolWindowModelKey
import java.io.File

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

        val module = findModuleForFile(e.getRequiredData(PlatformDataKeys.VIRTUAL_FILE), project)!!
        val moduleDir = PsiManager.getInstance(project).findDirectory(module.guessModuleDir()!!)!!

        val createProjectLicenseFile = {
            try {
                val licenseFile: PsiFile = WriteAction.compute(
                    ThrowableComputable<PsiFile, IncorrectOperationException> {
                        moduleDir.createFile(LICENSE_FILE_NAME)
                    }
                )

                val licenseDocument = PsiDocumentManager.getInstance(project).getDocument(licenseFile)!!
                val curProjectModule = model.projectModules.value.find { it.nativeModule == module }!!
                val compatibleLicenses = project.getUserData(ToolWindowModelKey)!!
                    .licenseManager.modulesCompatibleLicenses.value[curProjectModule]!!

                //TODO: Mb must be done in full order in licenses. Now the order is partial
                if (compatibleLicenses.any()) {
                    val recommendedLicense = compatibleLicenses[0]
                    application.runWriteAction {
                        licenseDocument.setText(recommendedLicense.fullText)
                    }
                    val newModulesLicenseMap = model.licenseManager.modulesLicenses.value.toMutableMap()
                    newModulesLicenseMap[curProjectModule] = recommendedLicense
                    model.licenseManager.modulesLicenses.set(newModulesLicenseMap)
                } else {
                    application.runWriteAction {
                        licenseDocument.setText(NoLicense.fullText)
                    }
                    val newModulesLicenseMap = model.licenseManager.modulesLicenses.value.toMutableMap()
                    newModulesLicenseMap[curProjectModule] = NoLicense
                    model.licenseManager.modulesLicenses.set(newModulesLicenseMap)
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

        val virtualFile = e.getData(PlatformDataKeys.VIRTUAL_FILE)
        if (virtualFile == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val module = findModuleForFile(virtualFile, project)
        if (module == null) {
            e.presentation.isEnabledAndVisible = false
            return
        }

        val moduleDirPath = module.guessModuleDir()
        if (moduleDirPath == null) {
            e.presentation.isEnabled = false
            return
        }

        val moduleDir = File(moduleDirPath.path)
        if (!moduleDir.exists() || !moduleDir.isDirectory || !moduleDir.canRead()) {
            e.presentation.isEnabled = false
            return
        }

        if (moduleDir.listFiles()!!.any { LICENSE_FILE_NAME_REGEX.matches(it.name) }) {
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