package net.morthen.gradle.multiloader.configurator.comon

import net.morthen.gradle.multiloader.api.LoaderConfigurator
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import net.morthen.gradle.multiloader.configurator.neoforge.ModDevGradle
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get

class CommonConfigurator(private val target: Project, private val legacy: Boolean, ext: MultiloaderExtension): LoaderConfigurator(ext) {

    override fun init() {
        super.init()

        if (legacy) initLegacy()
        else initModern()

        outgoingConfig()
    }

    private fun initModern() = with(target) {
        pluginManager.apply(ModDevGradle.PLUGIN_ID)

        extensions.configure<NeoForgeExtension> {
            neoFormVersion = settings.neoformVersion.get()

            val regular = getFile(target, "interface.json")
            if (regular.isPresent) interfaceInjectionData.from(regular.get().asFile.absolutePath)
        }
    }

    private fun initLegacy() = with(target) {
        pluginManager.apply(ModDevGradle.PLUGIN_LEGACY_ID)

        extensions.configure<LegacyForgeExtension> {
            mcpVersion = mcVersion
        }
    }

    private fun outgoingConfig() = with(target) {
        afterEvaluate {
            val commonJava = configurations.consumable("commonJava")
            val commonResources = configurations.consumable("commonResources")

            extensions.configure<JavaPluginExtension> {
                sourceSets["main"].resources.srcDir("src/generated")

                artifacts {
                    sourceSets["main"].java.sourceDirectories.forEach { add(commonJava.name, it) }
                    sourceSets["main"].resources.sourceDirectories.forEach { add(commonResources.name, it) }
                }
            }
        }
    }
}