package net.morthen.gradle.multiloader.plugins

import net.fabricmc.loom.LoomGradleExtension
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.morthen.gradle.multiloader.api.MinecraftVersion
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.*

object FabricLoom {
    const val PLUGIN_ID = "net.fabricmc.fabric-loom"
    const val PLUGIN_REMAP_ID = "net.fabricmc.fabric-loom-remap"

    fun get(obfuscated: Boolean): String {
        return if (obfuscated) PLUGIN_REMAP_ID else PLUGIN_ID
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

    loom.accessWidenerPath.convention(provider {
        val fileName = "${ ext.settings.modId.get() }.classtweaker"

        listOf(target, project(":common"))
            .flatMap { it.the(JavaPluginExtension::class).sourceSets["main"].resources.sourceDirectories.files }
            .flatMap { it.listFiles()?.toList() ?: listOf() }
            .firstOrNull { it.name == fileName }
    }.map { layout.projectDirectory.file(it.absolutePath) })

    //TODO: implement run configs
    loom.runConfigs {

    }

    //TODO: implement custom gradle tasks
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