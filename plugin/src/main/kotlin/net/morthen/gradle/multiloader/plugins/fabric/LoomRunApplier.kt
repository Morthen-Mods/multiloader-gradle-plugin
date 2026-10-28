package net.morthen.gradle.multiloader.plugins.fabric

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.morthen.gradle.multiloader.api.RunApplier
import net.morthen.gradle.multiloader.api.registerGradleRun
import net.morthen.gradle.multiloader.dsl.Run
import net.morthen.gradle.multiloader.dsl.RunType
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import org.gradle.api.GradleException
import org.gradle.api.Project

class LoomRunApplier(private val loom: LoomGradleExtensionAPI, private val ext: MultiloaderExtension): RunApplier {
    override fun apply(target: Project, run: Run) {
        val cfg = loom.runConfigs.findByName(run.getName()) ?: loom.runConfigs.create(run.getName())

        // Identity
        when (run.type.orNull) {
            RunType.CLIENT -> cfg.client()
            RunType.SERVER -> cfg.server()
            null -> throw GradleException("Missing runType for '${run.name}'")
        }

        run.displayName.orNull?.let { cfg.displayName.set(it) }
        run.mainClass.orNull?.let { cfg.mainClass.set(it) }
        cfg.sourceSet.set(run.sourceSetName)

        // System and JVM Settings
        cfg.runDirectory.set(ext.buildRunDirectory(target, run))
        cfg.programArguments.addAll(run.programArgs)
        cfg.jvmArguments.addAll(run.jvmArgs)
        cfg.systemProperties.putAll(run.systemProperties)
        cfg.environmentVars.putAll(run.environment)
        cfg.generateRunConfig.set(false)

        registerGradleRun(target, run)
    }
}