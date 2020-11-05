package com.jetbrains.licensedetector.intellij.plugin.module

import com.intellij.openapi.module.Module

enum class BuildSystemType(val statisticsKey: String) {
    MAVEN(statisticsKey = "maven"),
    GRADLE(statisticsKey = "gradle")
}

data class ProjectModule(
        val name: String,
        val nativeModule: Module,
        //TODO: Add parent
        //val parent: ProjectModule?,
        //val buildFile: VirtualFile?,
        //val buildSystemType: BuildSystemType,
        //val moduleType: ProjectModuleType
) {

    /*var getNavigatableDependency: (groupId: String, artifactId: String, version: String) -> Navigatable? =
        { _: String, _: String, _: String -> null }*/

    fun getFullName(): String {
        //TODO: Uncomment after add parent

        /* if (parent != null) {
             return parent.getFullName() + ":$name"
         }*/
        return name
    }
}
