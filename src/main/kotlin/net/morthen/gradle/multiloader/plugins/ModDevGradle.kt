package net.morthen.gradle.multiloader.plugins

import net.morthen.gradle.multiloader.api.MultiloaderExtension
import net.neoforged.moddevgradle.dsl.DataFileCollection
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the

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
        applyDefaultTransformer(target, this)

        runs {
            register("data") {
                clientData()
                systemProperty("terminal.ansi", "true")
                ideName.set("Common Datagen")
                gameDirectory.set(ext.runDir("data"))
                programArguments.set(listOf("--mod", ext.modId.get(), "--all", "--output", file("../common/src/generated").absolutePath, "--existing", file("../common/src/main/resources").absolutePath))
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

            register("client") {
                client()
                ideName.set("NeoForge Client")
                gameDirectory.set(ext.runDir("client"))
                programArguments.set(listOf("--username", ext.modAuthor.get()))

                sourceSet.set(java.sourceSets["main"])
                loadedMods.set(listOf(mods[ext.modId.get()]))
            }
            register("server") {
                server()
                ideName.set("NeoForge Server")
                gameDirectory.set(ext.runDir("server"))
                programArguments.set(listOf("--nogui"))

                sourceSet.set(java.sourceSets["main"])
                loadedMods.set(listOf(mods[ext.modId.get()]))
            }

            ext.testmodConfig?.let { testmod ->
                mods.create(testmod.modId.get()) { sourceSet(java.sourceSets[testmod.sourceSetName.get()]) }

                if (testmod.clientRun.get()) {
                    register("testmodClient") {
                        client()
                        ideName.set("NeoForge Test Client")
                        gameDirectory.set(ext.runDir("client"))
                        programArguments.set(listOf("--username", ext.modAuthor.get()))

                        sourceSet.set(testmod.sourceSetName.map { java.sourceSets[it] })
                        loadedMods.set(listOf(mods[ext.modId.get()], mods[testmod.modId.get()]))
                    }
                }

                if (testmod.serverRun.get()) {
                    register("testmodServer") {
                        server()
                        ideName.set("NeoForge Test Server")
                        gameDirectory.set(ext.runDir("server"))
                        programArguments.set(listOf("--nogui"))

                        sourceSet.set(testmod.sourceSetName.map { java.sourceSets[it] })
                        loadedMods.set(listOf(mods[ext.modId.get()], mods[testmod.modId.get()]))
                    }
                }
            }

            ext.gametestModConfig?.let { gametest ->
                mods.create(gametest.modId.get()) { sourceSet(java.sourceSets[gametest.sourceSetName.get()]) }

                register("gametestServer") {
                    type.set("gameTestServer")
                    ideName.set("NeoForge Game Test")
                    gameDirectory.set(ext.runDir("server"))
                    systemProperty("neoforge.enableGameTest", "true")

                    sourceSet.set(gametest.sourceSetName.map { java.sourceSets[it] })
                    loadedMods.set(listOf(mods[ext.modId.get()], mods[gametest.modId.get()]))
                }
            }
        }
    }
}