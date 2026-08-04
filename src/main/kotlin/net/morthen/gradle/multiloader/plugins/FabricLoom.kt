@file:Suppress("UnstableApiUsage")

package net.morthen.gradle.multiloader.plugins

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.LoomTasks
import net.morthen.gradle.multiloader.api.MultiloaderExtension
import net.morthen.gradle.multiloader.mcTransformer
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

object FabricLoom {
    const val LEGACY_PLUGIN_ID = "fabric-loom"
    const val PLUGIN_ID = "net.fabricmc.fabric-loom"
}

fun applyLoom(target: Project, ext: MultiloaderExtension) = with(target) {

    val extraDeps = configurations.dependencyScope("multiloaderExtraDeps")
    configurations.named("runtimeClasspath").configure { extendsFrom(extraDeps) }

    val loom = the(LoomGradleExtensionAPI::class)
    loom.accessWidenerPath.convention(provider { the(JavaPluginExtension::class).sourceSets["main"].resources.sourceDirectories.files
        .flatMap { it.listFiles()?.toList() ?: listOf() }
        .firstOrNull { it.name == "${ext.modId.get()}.classtweaker" }
    }.map { layout.projectDirectory.file(it.absolutePath) })

    afterEvaluate {
        if(ext.applySharedAccessTransforms.get()) {
            repositories.mcTransformer()
            dependencies {
                "implementation"("net.ashwork.mc:transformers:${ext.minecraftVersion.get()}.+")
            }
        }

        if(ext.loader.get() != "common") {
            loom.runConfigs {
                named("client") {
                    client()
                    displayName = "Fabric Client"
                    runDirectory = ext.runDir("client")
                }
                named("server") {
                    server()
                    displayName = "Fabric Server"
                    runDirectory = ext.runDir("server")
                }

                ext.testmodConfig?.let {
                    create("testmodClient") {
                        client()
                        displayName = "Fabric TestmodClient"
                        runDirectory = ext.runDir("testmod_client")
                        sourceSet = it.sourceSetName
                    }
                    create("testmodServer") {
                        server()
                        displayName = "Fabric TestmodServer"
                        runDirectory = ext.runDir("testmod_server")
                        sourceSet = it.sourceSetName
                    }
                }
            }
        }

        loom.runConfigs.configureEach {
            appendProjectPathToDisplayName = false

            systemProperties.put("fabric-tag-conventions-v2.missingTagTranslationWarning", "VERBOSE")
            systemProperties.put("mixin.debug", ext.mixinDebugRuns.map { it.toString() }.get())
            if(ext.loaderDebugRuns.get()) {
                systemProperties.put("fabric.log.level", "debug")
            }

            // register as Gradle runs instead of IDEA runs
            // https://github.com/FabricMC/fabric-loom/issues/1349
            generateRunConfig = false
            rootProject.pluginManager.apply("org.jetbrains.gradle.plugin.idea-ext")
            rootProject.the(IdeaModel::class).project.settings.runConfigurations.create<Gradle>(displayName.get()) {
                taskNames = listOf(LoomTasks.getRunConfigTaskName(this@configureEach))
                setProject(project)
            }
        }
    }
}

fun applyLoomMcGradleConventions(target: Project, loader: String, attribute: Attribute<String>) = with(target) {
    pluginManager.withPlugin(FabricLoom.PLUGIN_ID) {
        project.configurations.named("modCompileClasspath").configure {
            attributes { attribute(attribute, loader) }
        }
    }
}
