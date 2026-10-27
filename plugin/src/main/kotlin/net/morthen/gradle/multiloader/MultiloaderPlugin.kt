package net.morthen.gradle.multiloader

import net.morthen.gradle.multiloader.api.Loader
import net.morthen.gradle.multiloader.api.MinecraftVersion
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import net.morthen.gradle.multiloader.extensions.MultiloaderSettingsExtension
import net.morthen.gradle.multiloader.plugins.applyCommonModDev
import net.morthen.gradle.multiloader.plugins.fabric.applyFabricLoom
import net.morthen.gradle.multiloader.plugins.fabric.applyFabricLoomDatagen
import net.morthen.gradle.multiloader.plugins.applyForgeGradle
import net.morthen.gradle.multiloader.plugins.applyLegacyCommon
import net.morthen.gradle.multiloader.plugins.applyModDev
import net.morthen.gradle.multiloader.plugins.applyModDevDatagen
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.plugins.ide.idea.model.IdeaModel

class MultiloaderPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val mc = project.providers.gradleProperty("minecraft_version").get()
        MinecraftVersion.checkSupport(mc)

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

        // Base Java settings
        project.pluginManager.apply("java")
        project.extensions.getByType(JavaPluginExtension::class.java)
            .toolchain.languageVersion.set(settings.javaVersion.map { JavaLanguageVersion.of(it) })

        // Idea settings to download sources and javadoc
        project.pluginManager.apply("idea")
        val idea = project.extensions.getByType(IdeaModel::class.java)
        idea.module.isDownloadSources = true
        idea.module.isDownloadJavadoc = true

        project.afterEvaluate {
            val loader = ext.loader.get()
            val mc = settings.minecraftVersion.get()
            val legacy = MinecraftVersion.isLegacy(mc)

            when (loader) {
                Loader.COMMON -> {
                    if (legacy) applyLegacyCommon(project, ext)
                    else applyCommonModDev(project, ext)
                }
                Loader.FABRIC -> applyFabricLoom(project, ext)
                Loader.FORGE -> applyForgeGradle(project, ext)
                Loader.NEOFORGE -> {
                    if (legacy) throw GradleException("NeoForge does not support Minecraft $mc")
                    else applyModDev(project, ext)
                }
                Loader.DATAGEN -> {
                    if (legacy) applyFabricLoomDatagen(project, ext)
                    else applyModDevDatagen(project, ext)
                }
            }
        }
    }
}
