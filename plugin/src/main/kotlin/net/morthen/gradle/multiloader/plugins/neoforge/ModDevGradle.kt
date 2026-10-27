package net.morthen.gradle.multiloader.plugins.neoforge

import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

object ModDevGradle {
    const val PLUGIN_ID = "net.neoforged.moddev"
    const val PLUGIN_LEGACY_ID = "net.neoforged.moddev.legacyforge"
}

//TODO: - implement run configuration
//      - implement common project connections

// Used from 1.20.2 and beyond with neoform
fun applyCommonModDev(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(ModDevGradle.PLUGIN_ID)

    extensions.configure<NeoForgeExtension> {
        neoFormVersion = ext.settings.neoformVersion.get()
    }
}

// Used for common project until 1.20.1
fun applyLegacyCommon(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(ModDevGradle.PLUGIN_LEGACY_ID)

    extensions.configure<LegacyForgeExtension> {
        mcpVersion = ext.settings.minecraftVersion.get()
    }
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
        ext.settings.runs.forEach { ModDevRunApplier(this, target).apply(it) }

        runs {
            configureEach {
                systemProperty("terminal.ansi", "true")

                val upperName = name.replaceFirstChar { it.uppercase() }
                rootProject.the(IdeaModel::class).project.settings.runConfigurations.create<Gradle>("NeoForge $upperName") {
                    taskNames = listOf(":${project.name}:run$upperName")
                    setProject(project)
                }
            }
        }
    }
}

fun applyModDevDatagen(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(ModDevGradle.PLUGIN_ID)

    extensions.configure<NeoForgeExtension> {
        version = ext.settings.neoforgeVersion.get()
    }
}