package net.morthen.gradle.multiloader.plugins.obfuscated

import net.morthen.gradle.multiloader.plugins.LoaderConfigurator
import org.gradle.api.Project

object ModDevDataGradle : LoaderConfigurator {
    private const val PLUGIN_ID = "net.neoforged.moddev"

    override fun apply(project: Project) {
//        project.pluginManager.apply(PLUGIN_ID)

        println("[Mod Dev (Data) | Obfuscated] Applied version ${project.version}")
    }
}
