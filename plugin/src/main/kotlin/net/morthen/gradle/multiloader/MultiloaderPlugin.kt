package net.morthen.gradle.multiloader

import net.morthen.gradle.multiloader.api.Loader
import net.morthen.gradle.multiloader.api.MinecraftVersion
import net.morthen.gradle.multiloader.configurator.comon.CommonConfigurator
import net.morthen.gradle.multiloader.configurator.datagen.DatagenConfigurator
import net.morthen.gradle.multiloader.configurator.fabric.FabricConfigurator
import net.morthen.gradle.multiloader.configurator.forge.ForgeConfigurator
import net.morthen.gradle.multiloader.configurator.neoforge.NeoforgeConfigurator
import net.morthen.gradle.multiloader.conventions.applyCompileSettings
import net.morthen.gradle.multiloader.conventions.applyDefaultRepositories
import net.morthen.gradle.multiloader.conventions.applyGradleConvention
import net.morthen.gradle.multiloader.conventions.applyPlaceholderReplacement
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import net.morthen.gradle.multiloader.extensions.MultiloaderSettingsExtension
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.get
import org.gradle.plugins.ide.idea.model.IdeaModel

// TODO:    - Implement Maven Publish
//          - implement devLogin for client runs - native in neoforge -- Needs a big disclaimer in README because tokens are not encrypted
//          - implement ModPublish abstraction
//          - more to come
//

// TODO: Biggest TODO is to clean up the code, currently a mess

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

        project.applyDefaultRepositories()

        // Base Java settings
        val javaExt = project.extensions.getByType(JavaPluginExtension::class.java)
        javaExt.toolchain.languageVersion.set(settings.javaVersion.map { JavaLanguageVersion.of(it) })
        javaExt.withJavadocJar()
        javaExt.withSourcesJar()

        // version: 26.3-5.0.0
        // name: example_mod-neoforge
        // jar: example_mod-neoforge-26.3-5.0.0
        project.version = "${ settings.minecraftVersion.get() }-${ project.version }"
        project.extensions.getByType(BasePluginExtension::class.java)
            .archivesName.set("${ settings.modId.get() }-${ project.name }")

        // Idea settings to download sources and javadoc
        project.pluginManager.apply("idea")
        val idea = project.extensions.getByType(IdeaModel::class.java)
        idea.module.isDownloadSources = true
        idea.module.isDownloadJavadoc = true

        // Register CustomSource container
        ext.registerAdditionalSourceSets()

        project.afterEvaluate {
            val loader = ext.loader.get()
            val mc = settings.minecraftVersion.get()
            val legacy = MinecraftVersion.isLegacy(mc)

            applyGradleConvention(loader.name.lowercase())
            if (loader != Loader.COMMON) {
                applyCompileSettings(project, ext)
            }

            when (loader) {
                Loader.COMMON -> CommonConfigurator(project, legacy, ext).init()
                Loader.FABRIC -> FabricConfigurator(project, ext).init()
                Loader.FORGE -> ForgeConfigurator(project, ext).init()
                Loader.NEOFORGE -> NeoforgeConfigurator(project, legacy, ext).init()
                Loader.DATAGEN -> DatagenConfigurator(project, legacy, ext).init()
            }

            // Apply the custom placeholder replacements to the specified files
            project.applyPlaceholderReplacement(settings)

            // Prevent Testmod and gametest from being published and added as a module to the main source
            val javaComponent = components["java"] as AdhocComponentWithVariants
            listOfNotNull(settings.testmod?.sourceSet?.get(), settings.gametest?.sourceSet?.get()).forEach { sourceSetName ->
                javaComponent.withVariantsFromConfiguration(configurations["${sourceSetName}ApiElements"]) { skip() }
                javaComponent.withVariantsFromConfiguration(configurations["${sourceSetName}RuntimeElements"]) { skip() }
            }
        }
    }
}
