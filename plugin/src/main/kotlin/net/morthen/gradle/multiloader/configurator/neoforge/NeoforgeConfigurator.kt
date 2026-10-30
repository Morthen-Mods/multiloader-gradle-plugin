package net.morthen.gradle.multiloader.configurator.neoforge

import net.morthen.gradle.multiloader.api.LoaderPlugin
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the

class NeoforgeConfigurator(private val target: Project, private val legacy: Boolean, ext: MultiloaderExtension): LoaderPlugin(ext) {
    private lateinit var moddev: NeoForgeExtension

    override fun init() {
        super.init()
        if (legacy) throw GradleException("NeoForge does not support Minecraft $mcVersion")

        moddev = target.the<NeoForgeExtension>()
        applyAccessTransformer()
        applyInterfaceInjection()
        createMods()
        createRuns()
    }

    // Applies accesstransformer.cfg from the common project, the file from the project path itself get applied by default
    private fun applyAccessTransformer() = with(moddev) {
        target.project(":common").file("src/main/resources/META-INF/accesstransformer.cfg").let { accessTransformers.from(it.absolutePath) }
    }

    // Applies interface.json from common and target project, never gets applied by default
    private fun applyInterfaceInjection() = with(moddev) {
        target.project(":common").file("src/main/resources/META-INF/interface.json").let { interfaceInjectionData.from(it.absolutePath) }
        target.file("src/main/resources/META-INF/interface.json").let { interfaceInjectionData.from(it.absolutePath) }
    }

    // Creates all available mods, currently only 3 possible
    // TODO: Elaborate if more custom sources are reasonable, they should be then registered in a list
    private fun createMods() = with(moddev) {
        val java = target.the<JavaPluginExtension>()

        runs {
            mods.create(modId) { sourceSet(java.sourceSets["main"]) }
            settings.testmod?.let {
                mods.create(it.modId.get()) { sourceSet(java.sourceSets[it.sourceSet.get()]) }
            }
            settings.gametest?.let {
                mods.create(it.modId.get()) { sourceSet(java.sourceSets[it.sourceSet.get()]) }
            }
        }
    }

    private fun createRuns() = with(moddev) {
        settings.runs.forEach {
            ModDevRunApplier(this, ext).apply(target, it)
        }
    }
}