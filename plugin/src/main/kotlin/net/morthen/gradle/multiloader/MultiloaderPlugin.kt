package net.morthen.gradle.multiloader

import net.morthen.gradle.multiloader.api.Loader
import net.morthen.gradle.multiloader.api.MinecraftVersion
import net.morthen.gradle.multiloader.conventions.applyCompileSettings
import net.morthen.gradle.multiloader.conventions.applyGradleConvention
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import net.morthen.gradle.multiloader.extensions.MultiloaderSettingsExtension
import net.morthen.gradle.multiloader.plugins.neoforge.applyCommonModDev
import net.morthen.gradle.multiloader.plugins.fabric.applyFabricLoom
import net.morthen.gradle.multiloader.plugins.fabric.applyFabricLoomDatagen
import net.morthen.gradle.multiloader.plugins.forge.applyForgeGradle
import net.morthen.gradle.multiloader.plugins.neoforge.applyLegacyCommon
import net.morthen.gradle.multiloader.plugins.neoforge.applyModDev
import net.morthen.gradle.multiloader.plugins.neoforge.applyModDevDatagen
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.plugins.ide.idea.model.IdeaModel

// TODO:    - create boilerplate mods to test dependencies and runs
//          - implement proper common run creation/handling
//          - implement common connection to fabric/datagen/forge/neoforge
//          - implement devLogin for client runs - native in neoforge -- Needs a big disclaimer in README because tokens are not encrypted
//          - more to come

class MultiloaderPlugin : Plugin<Project> {
    private fun applyCommonPlugins(project: Project) {
        project.pluginManager.apply("java-library")
        project.pluginManager.apply("maven-publish")
        project.pluginManager.apply("version-catalog")
        project.pluginManager.apply("java")
        project.pluginManager.apply("idea")
        project.pluginManager.apply("org.jetbrains.gradle.plugin.idea-ext")
    }

    override fun apply(project: Project) {
        applyCommonPlugins(project)

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
        val javaExt = project.extensions.getByType(JavaPluginExtension::class.java)
        javaExt.toolchain.languageVersion.set(settings.javaVersion.map { JavaLanguageVersion.of(it) })
        javaExt.withJavadocJar()
        javaExt.withSourcesJar()

        // Idea settings to download sources and javadoc
        project.pluginManager.apply("idea")
        val idea = project.extensions.getByType(IdeaModel::class.java)
        idea.module.isDownloadSources = true
        idea.module.isDownloadJavadoc = true

        project.afterEvaluate {
            val loader = ext.loader.get()
            val mc = settings.minecraftVersion.get()
            val legacy = MinecraftVersion.isLegacy(mc)

            applyGradleConvention(loader.name.lowercase())
            if (loader != Loader.COMMON) {
                applyCompileSettings(project, ext)
            }

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
