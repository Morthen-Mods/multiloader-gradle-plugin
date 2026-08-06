package net.morthen.gradle.multiloader.plugins

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.morthen.gradle.multiloader.api.MultiloaderExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.the

object FabricLoom {
    const val PLUGIN_ID = "net.fabricmc.fabric-loom"
}

fun applyFabricLoom(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(FabricLoom.PLUGIN_ID)

    val loom = the<LoomGradleExtensionAPI>()

    dependencies {
        "minecraft"("com.mojang:minecraft:${ ext.minecraftVersion.get() }")
        "implementation"("net.fabricmc:fabric-loader:${ ext.fabricLoaderVersion.get() }")
        "implementation"("net.fabricmc.fabric-api:fabric-api:${ ext.fabricApiVersion.get() }")
    }

    loom.mods {
        create(ext.modId.get()) {
            sourceSet("main")
        }
    }
}