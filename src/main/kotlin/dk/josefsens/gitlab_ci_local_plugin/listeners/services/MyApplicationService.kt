package dk.josefsens.gitlab_ci_local_plugin.listeners.services

import dk.josefsens.gitlab_ci_local_plugin.MyBundle

class MyApplicationService {

    init {
        println(MyBundle.message("applicationService"))
    }
}
