package dk.josefsens.gitlab_ci_local_plugin.listeners.services

import com.intellij.openapi.project.Project
import dk.josefsens.gitlab_ci_local_plugin.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
