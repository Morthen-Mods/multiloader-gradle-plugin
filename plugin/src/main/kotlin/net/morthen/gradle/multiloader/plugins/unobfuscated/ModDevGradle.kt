package net.morthen.gradle.multiloader.plugins.unobfuscated

import net.morthen.gradle.multiloader.plugins.LoaderConfigurator
import org.gradle.api.Project

object ModDevGradle : LoaderConfigurator {
    private const val PLUGIN_ID = "net.neoforged.moddev"

    override fun apply(project: Project) {
        project.pluginManager.apply(PLUGIN_ID)
    }
}
