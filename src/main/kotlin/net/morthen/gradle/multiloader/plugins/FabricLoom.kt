package net.morthen.gradle.multiloader.plugins

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.LoomTasks
import net.morthen.gradle.multiloader.api.MultiloaderExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

object FabricLoom {
    const val PLUGIN_ID = "net.fabricmc.fabric-loom"
}

fun applyFabricLoom(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(FabricLoom.PLUGIN_ID)

    rootProject.pluginManager.apply("idea")
    rootProject.pluginManager.apply("org.jetbrains.gradle.plugin.idea-ext")

    val loom = the<LoomGradleExtensionAPI>()

    dependencies {
        "minecraft"("com.mojang:minecraft:${ ext.minecraftVersion.get() }")

        if (ext.fabricApiVersion.isPresent) {
            "implementation"("net.fabricmc.fabric-api:fabric-api:${ ext.fabricApiVersion.get() }")
        }

        if (ext.fabricLoaderVersion.isPresent) {
            "implementation"("net.fabricmc:fabric-loader:${ ext.fabricLoaderVersion.get() }")
        }
    }

    loom.accessWidenerPath.convention(provider {
        val fileName = "${ ext.modId.get() }.classtweaker"

        listOf(target, project(":common"))
            .flatMap { it.the(JavaPluginExtension::class).sourceSets["main"].resources.sourceDirectories.files }
            .flatMap { it.listFiles()?.toList() ?: listOf() }
            .firstOrNull { it.name == fileName }
    }.map { layout.projectDirectory.file(it.absolutePath) })

    loom.runConfigs {
        named("client") {
            client()
            displayName.set("Fabric Client")
            runDirectory.set(ext.runDir("client"))
            programArguments.set(listOf("--username", ext.modAuthor.get()))
        }

        named("server") {
            server()
            displayName.set("Fabric Server")
            runDirectory.set(ext.runDir("server"))
        }

        ext.testmodConfig?.let {
            if (it.clientRun.get()) {
                create("testmodClient") {
                    client()
                    displayName.set("Fabric Test Client")
                    runDirectory.set(ext.runDir("client"))
                    sourceSet.set(it.sourceSetName)
                    programArguments.set(listOf("--username", ext.modAuthor.get()))
                }
            }

            if (it.serverRun.get()) {
                create("testmodServer") {
                    server()
                    displayName.set("Fabric Test Server")
                    runDirectory.set(ext.runDir("server"))
                    sourceSet.set(it.sourceSetName)
                }
            }
        }

        ext.gametestModConfig?.let {
            create("gameTests") {
                server()
                systemProperties.put("fabric-api.gametest", "")
                displayName.set("Fabric Gametest Server")
                runDirectory.set(ext.runDir("server"))
                sourceSet.set(it.sourceSetName)
            }
        }
    }

    loom.runConfigs.configureEach {
        systemProperties.put("fabric-tag-conventions-v2.missingTagTranslationWarning", "VERBOSE")

        generateRunConfig.set(false)
        rootProject.the(IdeaModel::class).project.settings.runConfigurations.create<Gradle>(displayName.get()) {
            taskNames = listOf(LoomTasks.getRunConfigTaskName(this@configureEach))
            setProject(project)
        }
    }

    loom.mods {
        create(ext.modId.get()) {
            sourceSet("main")
        }

        ext.testmodConfig?.let {
            create(it.modId.get()) {
                sourceSet(it.sourceSetName.get())
            }
        }

        ext.gametestModConfig?.let {
            create(it.modId.get()) {
                sourceSet(it.sourceSetName.get())
            }
        }
    }
}