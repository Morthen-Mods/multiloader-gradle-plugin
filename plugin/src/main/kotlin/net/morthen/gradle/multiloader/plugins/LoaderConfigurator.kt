package net.morthen.gradle.multiloader.plugins

import net.morthen.gradle.multiloader.api.Loader
import net.morthen.gradle.multiloader.api.MinecraftVersion
import org.gradle.api.Project
import net.morthen.gradle.multiloader.plugins.obfuscated.FabricLoom as ObfFabricLoom
import net.morthen.gradle.multiloader.plugins.obfuscated.ForgeGradle as ObfForgeGradle
import net.morthen.gradle.multiloader.plugins.obfuscated.ModDevGradle as ObfModDevGradle
import net.morthen.gradle.multiloader.plugins.unobfuscated.FabricLoom as UnobfFabricLoom
import net.morthen.gradle.multiloader.plugins.unobfuscated.ForgeGradle as UnobfForgeGradle
import net.morthen.gradle.multiloader.plugins.unobfuscated.ModDevGradle as UnobfModDevGradle

/**
 * Applies and configures the third party loader plugin for a project.
 *
 * Implementations must only reference the plugin by its id, never by class, because the loader plugins are
 * compileOnly dependencies and may be missing at runtime.
 */
interface LoaderConfigurator {
    fun apply(project: Project)

    companion object {
        fun select(loader: Loader, minecraftVersion: String): LoaderConfigurator? {
            val obf = MinecraftVersion.isObfuscated(minecraftVersion)
            return when (loader) {
                Loader.COMMON, Loader.NEOFORGE -> if (obf) ObfModDevGradle else UnobfModDevGradle
                Loader.FABRIC -> if (obf) ObfFabricLoom else UnobfFabricLoom
                Loader.FORGE -> if (obf) ObfForgeGradle else UnobfForgeGradle
            }
        }
    }
}
