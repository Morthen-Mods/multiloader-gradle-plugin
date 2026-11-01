package net.morthen.gradle.multiloader.configurator.neoforge

import net.morthen.gradle.multiloader.api.Loader
import net.morthen.gradle.multiloader.api.MinecraftVersion
import net.morthen.gradle.multiloader.api.NativeRun
import net.morthen.gradle.multiloader.api.RunApplier
import net.morthen.gradle.multiloader.dsl.Run
import net.morthen.gradle.multiloader.dsl.RunType
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import net.neoforged.moddevgradle.dsl.ModDevExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.the

class NeoforgeRunApplier(target: Project, ext: MultiloaderExtension, private val modDev: ModDevExtension) : RunApplier(target, ext, Loader.NEOFORGE) {

    private val splitData = !MinecraftVersion.isBefore(mcVersion, "26.1")

    override fun loaderType(type: RunType) = when (type) {
        RunType.CLIENT -> "client"
        RunType.SERVER -> "server"
        RunType.CLIENT_DATA -> if (splitData) "clientData" else "data"
        RunType.SERVER_DATA -> if (splitData) "serverData" else "data"
        RunType.GAMETEST_SERVER -> "gameTestServer"
        RunType.CLIENT_GAMETEST -> null
    }

    override fun configure(run: Run, loaderType: String): NativeRun {
        val cfg = modDev.runs.maybeCreate(run.name)
        cfg.type.set(loaderType)
        cfg.disableIdeRun()

        run.mainClass.orNull?.let { cfg.mainClass.set(it) }
        cfg.sourceSet.set(target.the<JavaPluginExtension>().sourceSets.named(run.sourceSetName.get()))

        if (run.loadedMods.get().isEmpty()) {
            target.logger.error("Loaded mods for run '${run.name}' are empty, set at least the one modId with: loadedMods.add(modId)")
        } else {
            cfg.loadedMods.set(run.loadedMods.get().map { modDev.mods.getByName(it) })
        }

        cfg.gameDirectory.set(ext.buildRunDirectory(target, run))
        cfg.programArguments.addAll(run.programArgs)
        if (!run.type.get().client && !run.gui.get()) cfg.programArguments.add("nogui")
        cfg.jvmArguments.addAll(run.jvmArgs)
        run.maxHeapSize.orNull?.let { cfg.jvmArguments.add("-Xmx$it") }
        cfg.systemProperties.putAll(run.systemProperties)
        cfg.environment.putAll(run.environment)

        if (run.type.get() in setOf(RunType.CLIENT_DATA, RunType.SERVER_DATA)) {
            cfg.programArguments.addAll(datagenArgs(run.datagen))
        }

        if (run.type.get() == RunType.GAMETEST_SERVER) {
            cfg.systemProperty("neoforge.enableGameTest", "true")
        }

        return NativeRun("run" + run.name.replaceFirstChar { it.uppercase() }, cfg)
    }
}