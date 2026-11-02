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
import org.gradle.kotlin.dsl.the
import java.util.Locale
import java.util.Locale.getDefault

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

    // This should now configure all outgoing common sourcesets correctly
    private fun outgoingConfig() = with(target) {
        afterEvaluate {
            val java = the<JavaPluginExtension>()
            java.sourceSets["main"].resources.srcDir("src/generated")

            listOfNotNull("main", settings.testmod?.sourceSet?.get(), settings.gametest?.sourceSet?.get()).forEach { name ->
                val sourceSet = java.sourceSets[name]
                val prefix = if (name == "main") "common" else "common" + name.replaceFirstChar { it.uppercase() }

                val javaOut = configurations.consumable("${prefix}Java")
                val resOut = configurations.consumable("${prefix}Resources")

                artifacts {
                    sourceSet.java.sourceDirectories.forEach { add(javaOut.name, it) }
                    sourceSet.resources.sourceDirectories.forEach { add(resOut.name, it) }
                }
            }
        }
    }
}