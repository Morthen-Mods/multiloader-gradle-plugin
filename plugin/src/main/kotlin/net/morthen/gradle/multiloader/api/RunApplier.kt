package net.morthen.gradle.multiloader.api

import net.morthen.gradle.multiloader.dsl.DatagenSpec
import net.morthen.gradle.multiloader.dsl.Run
import net.morthen.gradle.multiloader.dsl.RunType
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

data class NativeRun(val taskName: String, val handle: Any)

/**
 * Base design for Run Abstraction was proposed by Claude Opus 5.5 implementation made by hand
 */
abstract class RunApplier(protected val target: Project, protected val ext: MultiloaderExtension, protected val loader: Loader) {
    protected val mcVersion: String = ext.settings.minecraftVersion.get()

    fun apply(run: Run) {
        // early return if the run should not be applied to this loader
        if (run.loaders.get().isNotEmpty() && loader !in run.loaders.get()) return

        val type = run.type.orNull ?: throw GradleException("Run '${run.name}' has no type")
        val loaderType = loaderType(type)
            ?: throw GradleException("Run '${run.name}': $type is not supported by $loader on Minecraft $mcVersion. Use onlyOn(...) to restrict it.")

        val native = configure(run, loaderType)
        run.loaderActions[loader]?.forEach { it.execute(native.handle) }

        registerGradleRun(target, run, native.taskName)
    }

    // returns the name of the runType,
    // if the loader doesn't support that type it returns null
    protected abstract fun loaderType(type: RunType): String?

    // Configures the run on the specific loader
    protected abstract fun configure(run: Run, loaderType: String): NativeRun

    // Registers the run as a usable Grade run
    protected fun registerGradleRun(target: Project, run: Run, taskName: String) {
        val project = target.name.replaceFirstChar { it.uppercase() }
        val upperName = run.name.replaceFirstChar { it.uppercase() }
        target.rootProject.the<IdeaModel>().project.settings.runConfigurations.create<Gradle>("$project " + run.displayName.orElse(upperName).get()) {
            taskNames = listOf("${target.path}:$taskName")
            setProject(target)
        }
    }

    // Only
    protected fun datagenArgs(spec: DatagenSpec): List<String> = buildList {
        addAll(listOf("--mod", spec.modId.get(), "--all", "--output", spec.outputDirectory.get().asFile.absolutePath))
        spec.existingResources.forEach { addAll(listOf("--existing", it.absolutePath)) }
    }
}