package net.morthen.gradle.multiloader

import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class MultiloaderPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        println("Multiloader plugin apply apply")
    }
}