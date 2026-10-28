package net.morthen.gradle.multiloader.plugins.fabric

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.morthen.gradle.multiloader.api.MinecraftVersion
import net.morthen.gradle.multiloader.conventions.applyGradleConvention
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the

object FabricLoom {
    const val PLUGIN_ID = "net.fabricmc.fabric-loom"
    const val PLUGIN_REMAP_ID = "net.fabricmc.fabric-loom-remap"

    fun get(obfuscated: Boolean): String {
        return if (obfuscated) PLUGIN_REMAP_ID else PLUGIN_ID
    }
}

fun applyFabricLoomDatagen(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(FabricLoom.PLUGIN_REMAP_ID)
    applyGradleConvention("fabric")

    val loom = the<LoomGradleExtensionAPI>()

    dependencies {
        "minecraft"("com.mojang:minecraft:${ ext.settings.minecraftVersion.get() }")
        "mappings"(loom.officialMojangMappings())
        "modImplementation"("net.fabricmc.fabric-api:fabric-api:${ ext.settings.fabricApiVersion.get() }")
        "modImplementation"("net.fabricmc:fabric-loader:${ ext.settings.fabricLoaderVersion.get() }")
    }
}

fun applyFabricLoom(target: Project, ext: MultiloaderExtension) = with(target){
    val obfuscated = MinecraftVersion.isObfuscated(ext.settings.minecraftVersion.get())
    pluginManager.apply(FabricLoom.get(obfuscated))

    val loom = the<LoomGradleExtensionAPI>()

    val implement = if (obfuscated) "modImplementation" else "implementation"
    dependencies {
        "minecraft"("com.mojang:minecraft:${ ext.settings.minecraftVersion.get() }")
        if (obfuscated) "mappings"(loom.officialMojangMappings())
        implement("net.fabricmc.fabric-api:fabric-api:${ ext.settings.fabricApiVersion.get() }")
        implement("net.fabricmc:fabric-loader:${ ext.settings.fabricLoaderVersion.get() }")
    }

    if (ext.modifierType.orNull != null) {
        loom.accessWidenerPath.convention(provider {
            val fileName = ext.modifierType.get().getFileName(ext.settings.modId.get())

            // searches in the fabric project and the common Project
            listOf(target, project(":common"))
                .flatMap { it.the(JavaPluginExtension::class).sourceSets["main"].resources.sourceDirectories.files }
                .flatMap { it.listFiles()?.toList() ?: listOf() }
                .firstOrNull { it.name == fileName }
        }.map { layout.projectDirectory.file(it.absolutePath) })
    }

    // Register Custom Runs
    ext.settings.runs.forEach { LoomRunApplier(loom, ext).apply(target, it) }

    // Apply tag convention property and prevent loom from generating Gradle runs
    loom.runConfigs.configureEach {
        systemProperties.put("fabric-tag-conventions-v2.missingTagTranslationWarning", "VERBOSE")
        generateRunConfig.set(false)
    }

    loom.mods {
        create(ext.settings.modId.get()) {
            sourceSet("main")
        }

        ext.settings.testmod?.let {
            create(it.modId.get()) {
                sourceSet(it.sourceSetName.get())
            }
        }

        ext.settings.gametest?.let {
            create(it.modId.get()) {
                sourceSet(it.sourceSetName.get())
            }
        }
    }
}