package net.morthen.gradle.multiloader.configurator.forge

import net.minecraftforge.gradle.ForgeGradleExtension
import net.minecraftforge.gradle.MinecraftExtension
import net.minecraftforge.gradle.MinecraftExtensionForProject
import net.morthen.gradle.multiloader.api.LoaderConfigurator
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.the

// TODO: Implement ForgeConfigurator properly when everything else works as intended - or eventually ditch forge
class ForgeConfigurator(private val target: Project, ext: MultiloaderExtension): LoaderConfigurator(ext) {
    private lateinit var java: JavaPluginExtension
    private lateinit var minecraft: MinecraftExtensionForProject

    override fun init() {
        super.init()

        target.pluginManager.apply(ForgeGradle.PLUGIN_ID)
        minecraft = target.the<MinecraftExtensionForProject>()
        java = target.the<JavaPluginExtension>()

        applyFixes()
        applyDependencies()
        applyAccessTransformer()
    }

    private fun applyDependencies() = with(target) {
        val repMC = the<MinecraftExtension>()
        val fg = the<ForgeGradleExtension>()

        val forge = ext.settings.forgeVersion.get()

        repositories {
            repMC.mavenizer(this)
            maven(fg.forgeMaven)
            maven(fg.minecraftLibsMaven)
            mavenCentral()
        }

        dependencies {
            "implementation"(minecraft.dependency("net.minecraftforge:forge:${ mcVersion }-${ forge }"))
        }
    }

    private fun applyFixes() = with(target) {
        java.sourceSets.configureEach {
            val dir = layout.buildDirectory.dir("sourcesSets/$name")
            output.setResourcesDir(dir.get())
            java.destinationDirectory.set(dir)
        }
    }

    private fun applyAccessTransformer() = with(minecraft) {
        val regular = getFile(target, "accesstransformer.cfg")
        if (regular.isPresent) accessTransformers.from(regular.get().asFile.absolutePath)
    }

    private fun configureBootstrap() {

    }

    private fun createRuns() {
        settings.runs.forEach {
            // TODO: implement new RunApplier
            println(it.displayName.get())
        }
    }
}