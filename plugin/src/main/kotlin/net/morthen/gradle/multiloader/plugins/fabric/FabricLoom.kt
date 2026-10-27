package net.morthen.gradle.multiloader.plugins.fabric

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.LoomTasks
import net.morthen.gradle.multiloader.api.MinecraftVersion
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

object FabricLoom {
    const val PLUGIN_ID = "net.fabricmc.fabric-loom"
    const val PLUGIN_REMAP_ID = "net.fabricmc.fabric-loom-remap"

    fun get(obfuscated: Boolean): String {
        return if (obfuscated) PLUGIN_REMAP_ID else PLUGIN_ID
    }
}

fun applyFabricLoomDatagen(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(FabricLoom.PLUGIN_REMAP_ID)

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

    //TODO: make accessWidener File a Property in MulitloaderSettingsExtension
    loom.accessWidenerPath.convention(provider {
        val fileName = "${ ext.settings.modId.get() }.classtweaker"

        listOf(target, project(":common"))
            .flatMap { it.the(JavaPluginExtension::class).sourceSets["main"].resources.sourceDirectories.files }
            .flatMap { it.listFiles()?.toList() ?: listOf() }
            .firstOrNull { it.name == fileName }
    }.map { layout.projectDirectory.file(it.absolutePath) })

    // Register Custom Runs
    ext.settings.runs.forEach {
        LoomRunApplier(loom).apply(it)
    }

    //TODO: implement custom gradle tasks - does those gradle task get implemented correctly?
    loom.runConfigs.configureEach {
        systemProperties.put("fabric-tag-conventions-v2.missingTagTranslationWarning", "VERBOSE")
        generateRunConfig.set(false)

        rootProject.the(IdeaModel::class).project.settings.runConfigurations.create<Gradle>(displayName.get()) {
            taskNames = listOf(LoomTasks.getRunConfigTaskName(this@configureEach))
            setProject(project)
        }
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