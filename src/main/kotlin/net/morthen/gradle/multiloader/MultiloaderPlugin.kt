@file:Suppress("UnstableApiUsage")

package net.morthen.gradle.multiloader

import net.morthen.gradle.multiloader.api.MultiloaderExtension
import net.morthen.gradle.multiloader.plugins.applyCommonModDevGradle
import net.morthen.gradle.multiloader.plugins.applyFabricLoom
import net.morthen.gradle.multiloader.plugins.applyForgeGradle
import net.morthen.gradle.multiloader.plugins.applyModDevGradle
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create

@Suppress("unused")
abstract class MultiloaderPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("java-library")

        val ext = extensions.create<MultiloaderExtension>("multiloader")

        // the `multiloader { }` block in the consumer's build script runs after apply(),
        // so ext.loader can only be read once the project has finished evaluating.
        afterEvaluate {
            val loader = ext.loader.get()

            when (loader) {
                "common" -> applyCommonModDevGradle(this@with, ext)
                "fabric" -> applyFabricLoom(this@with, ext)
                "forge" -> applyForgeGradle(this@with, ext)
                "neoforge" -> applyModDevGradle(this@with, ext)
                else -> throw GradleException("Unsupported multiloader.loader '$loader', expected one of: common, fabric, forge, neoforge")
            }
        }
    }
}
