package net.morthen.gradle.multiloader.configurator.neoforge

import net.morthen.gradle.multiloader.api.RunApplier
import net.morthen.gradle.multiloader.api.registerGradleRun
import net.morthen.gradle.multiloader.dsl.Run
import net.morthen.gradle.multiloader.dsl.RunType
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import net.neoforged.moddevgradle.dsl.ModDevExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.the

class ModDevRunApplier(private val modDev: ModDevExtension, private val ext: MultiloaderExtension): RunApplier {
    override fun apply(target: Project, run: Run) {
        val cfg = modDev.runs.findByName(run.name) ?: modDev.runs.create(run.name)

        // Identity
        when (run.type.orNull) {
            RunType.CLIENT -> cfg.client()
            RunType.SERVER -> cfg.server()
            null -> throw GradleException("Missing runType for '${run.name}'")
        }

        run.displayName.orNull?.let { cfg.ideName.set(it) }
        run.mainClass.orNull?.let { cfg.mainClass.set(it) }
        cfg.sourceSet.set(target.the<JavaPluginExtension>().sourceSets.named(run.sourceSetName.get()))

        // System and JVM Settings
        cfg.gameDirectory.set(ext.buildRunDirectory(target, run))
        cfg.programArguments.addAll(run.programArgs)
        cfg.jvmArguments.addAll(run.jvmArgs)
        cfg.systemProperties.putAll(run.systemProperties)
        cfg.environment.putAll(run.environment)
        cfg.disableIdeRun()

        run.loadedMods.get().forEach {
            cfg.loadedMods.add(modDev.mods.getByName(it))
        }

        registerGradleRun(target, run)
    }
}
