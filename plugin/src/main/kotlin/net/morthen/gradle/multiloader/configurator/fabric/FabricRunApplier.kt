package net.morthen.gradle.multiloader.configurator.fabric

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.task.LoomTasks
import net.morthen.gradle.multiloader.api.Loader
import net.morthen.gradle.multiloader.api.NativeRun
import net.morthen.gradle.multiloader.api.RunApplier
import net.morthen.gradle.multiloader.dsl.Run
import net.morthen.gradle.multiloader.dsl.RunType
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import org.gradle.api.Project

class FabricRunApplier(target: Project, ext: MultiloaderExtension, private val loom: LoomGradleExtensionAPI): RunApplier(target, ext, Loader.FABRIC) {
    override fun loaderType(type: RunType) = if (type.client) "client" else "server"

    override fun configure(run: Run, loaderType: String): NativeRun {
        val cfg = loom.runConfigs.maybeCreate(run.name)
        if (loaderType == "client") cfg.client() else cfg.server()     // server() setzt nogui
        if (!run.type.get().client && run.gui.get()) cfg.serverWithGui()

        run.mainClass.orNull?.let { cfg.mainClass.set(it) }
        cfg.sourceSet.set(run.sourceSetName)
        cfg.runDirectory.set(ext.buildRunDirectory(target, run))
        cfg.programArguments.addAll(run.programArgs)
        cfg.jvmArguments.addAll(run.jvmArgs)
        run.maxHeapSize.orNull?.let { cfg.jvmArguments.add("-Xmx$it") }
        cfg.systemProperties.putAll(run.systemProperties)
        cfg.environmentVars.putAll(run.environment)
        cfg.generateRunConfig.set(false)

        when (run.type.get()) {
            RunType.CLIENT_DATA, RunType.SERVER_DATA -> with(run.datagen) {
                cfg.systemProperties.put("fabric-api.datagen", "")
                cfg.systemProperties.put("fabric-api.datagen.output-dir", outputDirectory.get().asFile.absolutePath)
                cfg.systemProperties.put("fabric-api.datagen.modid", modId.get())
                if (strictValidation.getOrElse(false)) cfg.systemProperties.put("fabric-api.datagen.strict-validation", "true")
            }
            RunType.GAMETEST_SERVER -> cfg.systemProperties.put("fabric-api.gametest", "")
            RunType.CLIENT_GAMETEST -> cfg.systemProperties.put("fabric.client.gametest", "")
            else -> {}
        }

        return NativeRun(LoomTasks.getRunConfigTaskName(cfg), cfg)
    }
}