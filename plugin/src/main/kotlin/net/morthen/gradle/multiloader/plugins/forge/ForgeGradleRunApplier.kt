package net.morthen.gradle.multiloader.plugins.forge

import net.minecraftforge.gradle.MinecraftExtensionForProject
import net.morthen.gradle.multiloader.api.RunApplier
import net.morthen.gradle.multiloader.api.registerGradleRun
import net.morthen.gradle.multiloader.dsl.Run
import net.morthen.gradle.multiloader.dsl.RunType
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import org.gradle.api.GradleException
import org.gradle.api.Project

class ForgeGradleRunApplier(private val minecraft: MinecraftExtensionForProject, private val ext: MultiloaderExtension): RunApplier {
    override fun apply(target: Project, run: Run) {
        val cfg = minecraft.runs.findByName(run.name) ?: minecraft.runs.create(run.name)

        // Identity
        when (run.type.orNull) {
            RunType.CLIENT -> if (run.name != "client") cfg.configureBootstrapLaunch(true)
            RunType.SERVER -> if (run.name != "server") cfg.configureBootstrapLaunch(false)
            null -> throw GradleException("Missing runType for '${run.name}'")
        }

        run.mainClass.orNull?.let { cfg.mainClass.set(it) }

        // System and JVM Settings
        cfg.workingDir.set(ext.buildRunDirectory(target, run))
        cfg.args.addAll(run.programArgs)
        cfg.jvmArgs.addAll(run.jvmArgs)
        cfg.systemProperties.putAll(run.systemProperties)
        cfg.environment.putAll(run.environment)

        registerGradleRun(target, run)
    }
}