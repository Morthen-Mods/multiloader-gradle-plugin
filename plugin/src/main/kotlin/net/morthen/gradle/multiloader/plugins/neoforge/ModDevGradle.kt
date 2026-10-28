package net.morthen.gradle.multiloader.plugins.neoforge

import net.morthen.gradle.multiloader.api.registerGradleRun
import net.morthen.gradle.multiloader.conventions.applyGradleConvention
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the

object ModDevGradle {
    const val PLUGIN_ID = "net.neoforged.moddev"
    const val PLUGIN_LEGACY_ID = "net.neoforged.moddev.legacyforge"
}

//TODO: - implement run configuration
//      - implement common project connections

private fun outgoingConfig(target: Project) = with(target) {
    afterEvaluate {
        val commonJava = configurations.consumable("commonJava")
        val commonResources = configurations.consumable("commonResources")

        extensions.configure<JavaPluginExtension> {
            sourceSets["main"].resources.srcDir("src/generated")

            artifacts {
                sourceSets["main"].java.sourceDirectories.forEach { add(commonJava.name, it) }
                sourceSets["main"].resources.sourceDirectories.forEach { add(commonResources.name, it) }
            }
        }
    }
}

// Used from 1.20.2 and beyond with neoform
fun applyCommonModDev(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(ModDevGradle.PLUGIN_ID)

    extensions.configure<NeoForgeExtension> {
        neoFormVersion = ext.settings.neoformVersion.get()
    }

    outgoingConfig(this)
}

// Used for common project until 1.20.1
fun applyLegacyCommon(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(ModDevGradle.PLUGIN_LEGACY_ID)

    extensions.configure<LegacyForgeExtension> {
        mcpVersion = ext.settings.minecraftVersion.get()
    }

    outgoingConfig(this)
}

// Neoforge ModDev after 1.20.2
fun applyModDev(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(ModDevGradle.PLUGIN_ID)

    extensions.configure<NeoForgeExtension> {
        version = ext.settings.neoforgeVersion.get()
        val java = the<JavaPluginExtension>()

        // Register Mods
        runs {
            mods.create(ext.settings.modId.get()) { sourceSet(java.sourceSets["main"]) }
            ext.settings.testmod?.let {
                mods.create(it.modId.get()) { sourceSet(java.sourceSets[it.sourceSetName.get()]) }
            }

            ext.settings.gametest?.let {
                mods.create(it.modId.get()) { sourceSet(java.sourceSets[it.sourceSetName.get()]) }
            }
        }

        // Register runs
        ext.settings.runs.forEach { ModDevRunApplier(this, ext).apply(target, it) }

        runs {
            configureEach {
                systemProperty("terminal.ansi", "true")
            }
        }
    }
}

fun applyModDevDatagen(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(ModDevGradle.PLUGIN_ID)
    applyGradleConvention("neoforge")

    extensions.configure<NeoForgeExtension> {
        version = ext.settings.neoforgeVersion.get()
    }
}