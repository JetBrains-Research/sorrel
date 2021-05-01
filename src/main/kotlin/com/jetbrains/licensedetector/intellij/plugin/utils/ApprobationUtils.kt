package com.jetbrains.licensedetector.intellij.plugin.utils

import com.intellij.ide.impl.ProjectUtil
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import kotlinx.coroutines.runBlocking
import org.jetbrains.idea.maven.project.MavenProjectsManager
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File
import java.nio.file.Paths

object ApprobationUtils {

    /**
     * Prefix for files with module compatibility issue results
     */
    const val modulePathResultPrefix = "module_"

    /**
     * Prefix for files with library compatibility issue results
     */
    const val libraryPathResultPrefix = "library_"

    /**
     * @param projectPath path to test project
     */
    fun setUpProject(projectPath: String): Project {
        val project: Project = ProjectUtil.openOrImport(Paths.get(projectPath))

        if (MavenProjectsManager.getInstance(project).isMavenizedProject) {
            MavenProjectsManager.getInstance(project).scheduleImportAndResolve()
            MavenProjectsManager.getInstance(project).importProjects()
        } else {
            ExternalSystemUtil.refreshProject(
                projectPath,
                ImportSpecBuilder(project, GradleConstants.SYSTEM_ID)
            )
        }

        return project
    }

    /**
     * @param project test project
     * @param pathForResultDir Path to the folder where the approbation result is saved
     */
    fun doApprobationTest(project: Project, pathForResultDir: String) {
        runBlocking {
            println("Project name: " + project.name)
            println("----------Project modules and their libs----------")
            ModuleManager.getInstance(project).modules.forEach {
                println("Module name: " + it.name)
                println("Module's libs")
                ModuleRootManager.getInstance(it).orderEntries().forEachLibrary { lib ->
                    println("Library " + lib.name)
                    true
                }
            }

            val licenseDetectorModel = project.licenseDetectorModel()
            licenseDetectorModel.requestRefreshContext
            licenseDetectorModel.dataChangeJob?.join()
            exportCollectedIssues(project, pathForResultDir)
        }
    }

    /**
     * Export collected compatibility issues to separate files
     */
    fun exportCollectedIssues(project: Project, pathForResultDir: String) {
        val licenseDetectorModel = project.licenseDetectorModel()
        println("Root project module: " + licenseDetectorModel.rootModule.value?.name)
        println("----------License Compatibility issues----------")
        println("---Dependency Issues---")
        val libraryIssues = licenseDetectorModel.licenseManager.compatibilityIssues.value
            .convertPackageDependencyIssueGroupsToPlainText()
        println(libraryIssues)
        val libraryIssuesFile = File(
            pathForResultDir + "\\" + libraryPathResultPrefix + cleanProjectName(project.name) + ".txt"
        )
        libraryIssuesFile.writeText(libraryIssues)
        println("---Module Issues---")
        val moduleIssues = licenseDetectorModel.licenseManager.compatibilityIssues.value
            .convertSubmodulesIssueGroupsToPlainText()
        println(moduleIssues)
        val moduleIssuesFile = File(
            pathForResultDir + "\\" + modulePathResultPrefix + cleanProjectName(project.name) + ".txt"
        )
        moduleIssuesFile.writeText(moduleIssues)
    }

    fun cleanProjectName(projectName: String): String {
        return projectName.replace(Regex("\\W"), "_")
    }

    fun processPathForInsertion(path: String): String {
        return path.replace("\\", "\\\\")
    }
}