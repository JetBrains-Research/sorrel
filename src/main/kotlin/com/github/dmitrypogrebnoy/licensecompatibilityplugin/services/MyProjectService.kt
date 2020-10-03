package com.github.dmitrypogrebnoy.licensecompatibilityplugin.services

import com.intellij.openapi.project.Project
import com.github.dmitrypogrebnoy.licensecompatibilityplugin.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
