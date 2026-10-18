package net.morthen.gradle.multiloader

import org.gradle.api.Plugin
import org.gradle.api.Project

class MultiloaderPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.logger.lifecycle("Multiloader plugin applied to ${project.path}")
    }
}
