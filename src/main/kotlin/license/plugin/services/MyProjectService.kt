package license.plugin.services

import com.intellij.openapi.project.Project
import license.plugin.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
