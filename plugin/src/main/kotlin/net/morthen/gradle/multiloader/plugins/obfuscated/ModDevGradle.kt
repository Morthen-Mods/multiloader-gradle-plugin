package net.morthen.gradle.multiloader.plugins.obfuscated

import net.morthen.gradle.multiloader.plugins.LoaderConfigurator
import org.gradle.api.Project

object ModDevGradle : LoaderConfigurator {
    private const val PLUGIN_ID = "net.neoforged.moddev"

    override fun apply(project: Project) {
//        project.pluginManager.apply(PLUGIN_ID)

        println("[Mod Dev | Obfuscated] Applied version ${project.version}")
    }
}
