package net.morthen.gradle.multiloader.configurator.forge

import net.minecraftforge.gradle.MinecraftExtensionForProject
import net.minecraftforge.gradle.SlimeLauncherOptionsNested
import net.morthen.gradle.multiloader.api.Loader
import net.morthen.gradle.multiloader.api.NativeRun
import net.morthen.gradle.multiloader.api.RunApplier
import net.morthen.gradle.multiloader.dsl.Run
import net.morthen.gradle.multiloader.dsl.RunType
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.the

class ForgeRunApplier(target: Project, ext: MultiloaderExtension, private val minecraft: MinecraftExtensionForProject): RunApplier(target, ext, Loader.FORGE) {

    private val used = mutableMapOf<Pair<String, String>, String>()   // (typ, sourceSet) -> Plugin-Run

    override fun loaderType(type: RunType) = when (type) {
        RunType.CLIENT -> "client"
        RunType.SERVER -> "server"
        RunType.CLIENT_DATA, RunType.SERVER_DATA -> "data" // Forge only knows data
        RunType.GAMETEST_SERVER -> "gameTestServer"
        RunType.CLIENT_GAMETEST -> null
    }

    override fun configure(run: Run, loaderType: String): NativeRun {
        val sourceSetName = run.sourceSetName.get()
        used.put(loaderType to sourceSetName, run.name)?.let {
            throw GradleException("Forge allows one run per type and source set: '${run.name}' and '$it' both are $loaderType on $sourceSetName")
        }

        val base = minecraft.runs.maybeCreate(loaderType)
        lateinit var nested: SlimeLauncherOptionsNested
        base.with(sourceSetName) {
            nested = this
            run.mainClass.orNull?.let { mainClass.set(it) }
            workingDir.set(ext.buildRunDirectory(target, run))
            args(run.programArgs)

            // Forge needs mixins as args
            ext.mixins.get().forEach { mixin -> args("--mixin.config=$mixin") }

            if (!run.type.get().client && !run.gui.get()) args("nogui")
            jvmArgs(run.jvmArgs)
            run.maxHeapSize.orNull?.let { maxHeapSize.set(it) }
            systemProperties(run.systemProperties)
            environment(run.environment)
            if (run.type.get() in setOf(RunType.CLIENT_DATA, RunType.SERVER_DATA)) args(datagenArgs(run.datagen))
        }

        val sourceSet = target.the<JavaPluginExtension>().sourceSets.getByName(sourceSetName)
        return NativeRun(sourceSet.getTaskName("run", loaderType), nested)
    }
}