package com.jetbrains.licensedetector.intellij.plugin

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.ui.Messages
import java.util.*


class ShowDependencyAction : AnAction("Project Dependency") {
    override fun actionPerformed(e: AnActionEvent) {
        e.project ?: return

        val application = ApplicationManager.getApplication()
        val strBuilder: StringBuilder = StringBuilder()


        ModuleManager.getInstance(
                e.project!!
        ).modules.forEach { module ->

            val dependenciesNames: MutableList<String> = ArrayList()

            strBuilder.append(module.name)
            strBuilder.append("\n")

            ModuleRootManager.getInstance(
                    module
            ).orderEntries().forEachLibrary { library: Library ->
                val simplifiedId = library.getSimpleIdentifier()
                if (simplifiedId != null) {
                    dependenciesNames.add(simplifiedId)
                }
                true
            }

            //TODO: Should exec in application.executeOnPooledThread
            application.executeOnPooledThread {
                val licensesDependencies = DependencyLicenseFinder.getLicensesForDependencies(dependenciesNames)

                dependenciesNames.forEach {
                    strBuilder.append(it)
                    strBuilder.append(" - ")
                    if (licensesDependencies.containsKey(it)) {
                        strBuilder.append(licensesDependencies[it])
                    } else {
                        strBuilder.append("No info")
                    }
                    strBuilder.append("\n")
                }

                strBuilder.append("\n")
                strBuilder.append("\n")
            }.get()

        }


        //TODO: Just for test show
        Messages.showInfoMessage(strBuilder.toString(), "Dependencies licenses info")

    }

    fun Library.getSimpleIdentifier(): String? {
        val name: String = this.name ?: return null
        return name.substringAfter(' ').substringBeforeLast(':')
    }
}