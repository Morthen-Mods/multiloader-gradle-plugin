package net.morthen.gradle.multiloader.plugins.obfuscated

import net.morthen.gradle.multiloader.plugins.LoaderConfigurator
import org.gradle.api.Project

object FabricLoom : LoaderConfigurator {
    private const val PLUGIN_ID = "net.fabricmc.fabric-loom-remap"

    override fun apply(project: Project) {
//        project.pluginManager.apply(PLUGIN_ID)

        println("[Fabric Loom | Obfuscated] Applied version ${project.version}")
    }
}
