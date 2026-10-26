package net.morthen.gradle.multiloader.plugins.unobfuscated

import net.morthen.gradle.multiloader.plugins.LoaderConfigurator
import org.gradle.api.Project

object ForgeGradle : LoaderConfigurator {
    private const val PLUGIN_ID = "net.minecraftforge.gradle"

    override fun apply(project: Project) {
//        project.pluginManager.apply(PLUGIN_ID)

        println("[Forge Gradle] Applied version ${project.version}")
    }
}
