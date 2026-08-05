package net.morthen.gradle.multiloader.plugins

import net.morthen.gradle.multiloader.api.MultiloaderExtension
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

object ModDevGradle {
    const val PLUGIN_ID = "net.neoforged.moddev"
}

fun applyCommonModDevGradle(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(ModDevGradle.PLUGIN_ID)
    extensions.configure<NeoForgeExtension> {
        neoFormVersion = ext.neoFormVersion.get()
    }
}

fun applyModDevGradle(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(ModDevGradle.PLUGIN_ID)

    extensions.configure<NeoForgeExtension> {
        version = ext.neoForgeVersion.get()
    }
}