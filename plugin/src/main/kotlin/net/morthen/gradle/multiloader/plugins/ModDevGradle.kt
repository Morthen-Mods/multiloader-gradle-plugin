package net.morthen.gradle.multiloader.plugins

import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

object ModDevGradle {
    const val PLUGIN_ID = "net.neoforged.moddev"
    const val PLUGIN_LEGACY_ID = "net.neoforged.moddev.legacyforge"
}

//TODO: - implement run configuration
//      - implement common project connections
//      -

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
    }
}

fun applyModDevDatagen(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(ModDevGradle.PLUGIN_ID)

    extensions.configure<NeoForgeExtension> {
        version = ext.settings.neoforgeVersion.get()
    }
}