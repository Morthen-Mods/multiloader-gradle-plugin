package net.morthen.gradle.multiloader.plugins

import net.morthen.gradle.multiloader.api.MultiloaderExtension
import net.neoforged.moddevgradle.dsl.DataFileCollection
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
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
}

fun addIfExists(collection: DataFileCollection, target: Project, relativePath: String) {
    val commonFile = target.project(":common").file(relativePath)
    val selfFile = target.file(relativePath)

    if (commonFile.exists()) collection.from(commonFile)
    if (selfFile.exists()) collection.from(selfFile)
}

fun applyDefaultTransformer(target: Project, extension: NeoForgeExtension) {
    extension.validateAccessTransformers.set(true)

    addIfExists(extension.accessTransformers, target, "src/main/resources/META-INF/accesstransformer.cfg")
    addIfExists(extension.interfaceInjectionData, target, "src/main/resources/META-INF/interface.json")
}

fun applyCommonModDevGradle(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(ModDevGradle.PLUGIN_ID)

    extensions.configure<NeoForgeExtension> {
        neoFormVersion = ext.neoFormVersion.get()
        applyDefaultTransformer(target, this)
    }

    afterEvaluate {
        val commonJava = configurations.consumable("commonJava")
        val commonResources = configurations.consumable("commonResources")

        extensions.configure<JavaPluginExtension> {
            sourceSets["main"].resources.srcDir("src/generated")

            artifacts {
                sourceSets["main"].java.sourceDirectories.forEach { add(commonJava.name, it) }
                sourceSets["main"].resources.sourceDirectories.forEach { add(commonResources.name, it) }
            }
        }
    }
}

fun applyDatagenModDevGradle(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(ModDevGradle.PLUGIN_ID)

    extensions.configure<NeoForgeExtension> {
        version = ext.neoForgeVersion.get()
        applyDefaultTransformer(target, this)

        runs {
            configureEach {
                disableIdeRun()
                systemProperty("terminal.ansi", "true")
            }

            register("data") {
                clientData()
                gameDirectory.set(ext.runDir("data"))
                programArguments.set(listOf("--mod", ext.modId.get(), "--all", "--output", file("../common/src/generated").absolutePath, "--existing", file("../common/src/main/resources").absolutePath))

                rootProject.the(IdeaModel::class).project.settings.runConfigurations.create<Gradle>("Common Datagen") {
                    taskNames = listOf(":${project.name}:runData")
                    setProject(project)
                }
            }
        }

        mods.create(ext.modId.get()) { sourceSet(the<JavaPluginExtension>().sourceSets["main"]) }
    }
}

fun applyModDevGradle(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(ModDevGradle.PLUGIN_ID)

    extensions.configure<NeoForgeExtension> {
        version = ext.neoForgeVersion.get()
        applyDefaultTransformer(target, this)

        val java = the<JavaPluginExtension>()

        runs {
            mods.create(ext.modId.get()) { sourceSet(java.sourceSets["main"]) }
            configureEach {
                disableIdeRun()
                systemProperty("terminal.ansi", "true")
            }

            register("client") {
                client()
                gameDirectory.set(ext.runDir("client"))
                programArguments.set(listOf("--username", ext.modAuthor.get()))

                sourceSet.set(java.sourceSets["main"])
                loadedMods.set(listOf(mods[ext.modId.get()]))

                rootProject.the(IdeaModel::class).project.settings.runConfigurations.create<Gradle>("NeoForge Client") {
                    taskNames = listOf(":${project.name}:runClient")
                    setProject(project)
                }
            }
            register("server") {
                server()
                gameDirectory.set(ext.runDir("server"))
                programArguments.set(listOf("--nogui"))

                sourceSet.set(java.sourceSets["main"])
                loadedMods.set(listOf(mods[ext.modId.get()]))

                rootProject.the(IdeaModel::class).project.settings.runConfigurations.create<Gradle>("NeoForge Server") {
                    taskNames = listOf(":${project.name}:runServer")
                    setProject(project)
                }
            }

            ext.testmodConfig?.let { testmod ->
                mods.create(testmod.modId.get()) { sourceSet(java.sourceSets[testmod.sourceSetName.get()]) }

                if (testmod.clientRun.get()) {
                    register("testmodClient") {
                        client()
                        gameDirectory.set(ext.runDir("client"))
                        programArguments.set(listOf("--username", ext.modAuthor.get()))

                        sourceSet.set(testmod.sourceSetName.map { java.sourceSets[it] })
                        loadedMods.set(listOf(mods[ext.modId.get()], mods[testmod.modId.get()]))

                        rootProject.the(IdeaModel::class).project.settings.runConfigurations.create<Gradle>("NeoForge Test Client") {
                            taskNames = listOf(":${project.name}:runTestmodClient")
                            setProject(project)
                        }
                    }
                }

                if (testmod.serverRun.get()) {
                    register("testmodServer") {
                        server()
                        gameDirectory.set(ext.runDir("server"))
                        programArguments.set(listOf("--nogui"))

                        sourceSet.set(testmod.sourceSetName.map { java.sourceSets[it] })
                        loadedMods.set(listOf(mods[ext.modId.get()], mods[testmod.modId.get()]))

                        rootProject.the(IdeaModel::class).project.settings.runConfigurations.create<Gradle>("NeoForge Test Server") {
                            taskNames = listOf(":${project.name}:runTestmodServer")
                            setProject(project)
                        }
                    }
                }
            }

            ext.gametestModConfig?.let { gametest ->
                mods.create(gametest.modId.get()) { sourceSet(java.sourceSets[gametest.sourceSetName.get()]) }

                register("gametestServer") {
                    type.set("gameTestServer")
                    gameDirectory.set(ext.runDir("server"))
                    systemProperty("neoforge.enableGameTest", "true")

                    sourceSet.set(gametest.sourceSetName.map { java.sourceSets[it] })
                    loadedMods.set(listOf(mods[ext.modId.get()], mods[gametest.modId.get()]))

                    rootProject.the(IdeaModel::class).project.settings.runConfigurations.create<Gradle>("NeoForge Gametest") {
                        taskNames = listOf(":${project.name}:runGametestServer")
                        setProject(project)
                    }
                }
            }
        }
    }
}