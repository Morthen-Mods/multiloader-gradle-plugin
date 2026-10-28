package net.morthen.gradle.multiloader.api

import net.morthen.gradle.multiloader.dsl.Run
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

interface RunApplier {
    fun apply(run: Run)
}

fun registerGradleRun(target: Project, run: Run){
    val upperName = run.name.replaceFirstChar { it.uppercase() }
    val project = target.name.replaceFirstChar { it.uppercase() }
    target.rootProject.the(IdeaModel::class).project.settings.runConfigurations.create<Gradle>( "$project " + run.displayName.orElse(upperName).get()) {
        taskNames = listOf(":${target.name}:run$upperName")
        setProject(target)
    }
}