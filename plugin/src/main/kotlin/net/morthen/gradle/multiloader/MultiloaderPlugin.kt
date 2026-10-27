package net.morthen.gradle.multiloader

import net.morthen.gradle.multiloader.api.Loader
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import net.morthen.gradle.multiloader.extensions.MultiloaderSettingsExtension
import net.morthen.gradle.multiloader.plugins.applyFabricLoom
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

        val ext = project.extensions.create("multiloader", MultiloaderExtension::class.java)
        ext.settings = settings

        project.pluginManager.apply("java")
        project.extensions.getByType(JavaPluginExtension::class.java)
            .toolchain.languageVersion.set(settings.javaVersion.map { JavaLanguageVersion.of(it) })

        project.afterEvaluate {
            val loader = ext.loader.get()

            if (loader == Loader.FABRIC) applyFabricLoom(project, ext)
        }
    }
}
