package net.morthen.gradle.multiloader.plugins

import net.morthen.gradle.multiloader.api.MultiloaderExtension
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get

object ModDevGradle {
    const val PLUGIN_ID = "net.neoforged.moddev"
}

fun applyCommonModDevGradle(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(ModDevGradle.PLUGIN_ID)
    extensions.configure<NeoForgeExtension> {
        neoFormVersion = ext.neoFormVersion.get()
    }

    afterEvaluate {
        val commonJava = configurations.consumable("commonJava")
        val commonResources = configurations.consumable("commonResources")

        extensions.configure<JavaPluginExtension> {
            artifacts {
                sourceSets["main"].java.sourceDirectories.forEach { add(commonJava.name, it) }
                sourceSets["main"].resources.sourceDirectories.forEach { add(commonResources.name, it) }
            }
        }

        fun configureArtifacts(sourceSet: String) {
            val commonJava = configurations.consumable("${sourceSet}CommonJava")
            val commonResources = configurations.consumable("${sourceSet}CommonResources")

            extensions.configure<JavaPluginExtension> {
                artifacts {
                    sourceSets[sourceSet].java.sourceDirectories.forEach { add(commonJava.name, it) }
                    sourceSets[sourceSet].resources.sourceDirectories.forEach { add(commonResources.name, it) }
                }
            }
        }

        ext.testmodConfig?.let {
            configureArtifacts(it.sourceSetName.get())
        }

        ext.gametestModConfig?.let {
            configureArtifacts(it.sourceSetName.get())
        }
    }
}

fun applyDatagenModDevGradle(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(ModDevGradle.PLUGIN_ID)

    extensions.configure<NeoForgeExtension> {
        version = ext.neoForgeVersion.get()
    }
}

fun applyModDevGradle(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(ModDevGradle.PLUGIN_ID)

    extensions.configure<NeoForgeExtension> {
        version = ext.neoForgeVersion.get()
    }
}