package net.morthen.gradle.multiloader

import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import net.morthen.gradle.multiloader.extensions.MultiloaderSettingsExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion

class MultiloaderPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        if (project == project.rootProject) {
            project.extensions.create("multiloaderSettings", MultiloaderSettingsExtension::class.java)
        } else {
            applyToSubproject(project)
        }
    }

    private fun applyToSubproject(project: Project) {
        val settings = project.rootProject.extensions.findByType(MultiloaderSettingsExtension::class.java)
            ?: throw GradleException("Apply 'net.morthen.gradle.multiloader' to the root project as well (project ${project.path})")

        val extension = project.extensions.create("multiloader", MultiloaderExtension::class.java)
        extension.minecraftVersion.convention(settings.minecraftVersion)
        extension.javaVersion.convention(settings.javaVersion)

        project.pluginManager.withPlugin("java") {
            project.extensions.getByType(JavaPluginExtension::class.java)
                .toolchain.languageVersion.set(extension.javaVersion.map { JavaLanguageVersion.of(it) })
        }

        project.afterEvaluate {
            val loader = extension.loader.orNull ?: return@afterEvaluate

        }
    }
}
