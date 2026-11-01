package net.morthen.gradle.multiloader.configurator.forge

import net.minecraftforge.gradle.ForgeGradleExtension
import net.minecraftforge.gradle.MinecraftExtension
import net.minecraftforge.gradle.MinecraftExtensionForProject
import net.morthen.gradle.multiloader.api.LoaderConfigurator
import net.morthen.gradle.multiloader.dsl.CustomSource
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.Copy
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*

/**
 * ForgeConfigurator implementation is 50% based on Claude Opus 5.5, since ForgeGradle is a little headache to implement
 */
class ForgeConfigurator(private val target: Project, ext: MultiloaderExtension): LoaderConfigurator(ext) {
    private lateinit var minecraft: MinecraftExtensionForProject
    private lateinit var java: JavaPluginExtension

    override fun init() {
        super.init()

        target.pluginManager.apply(ForgeGradle.PLUGIN_ID)
        minecraft = target.the<MinecraftExtensionForProject>()
        java = target.the<JavaPluginExtension>()

        applyFixes()
        applyDependencies()
        applyAccessTransformer()

        // This implementation is based on Claude Opus, wasn't tempted enough to deal with Forge that much
        inheritMainConfigurations()
        settings.gametest?.let { applyGametestData(it) }

        buildManifest()
        createRuns()
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

    private fun createRuns() {
        settings.runs.forEach { ForgeRunApplier(target, ext, minecraft).apply(it) }
    }

    private fun buildManifest() {
        target.tasks.named<Jar>("jar") {
            manifest {
                attributes(mapOf<String, Any>(
                    "MixinConfig" to ext.mixins.map { it.joinToString(",") }
                ))
            }
        }
    }

    private fun inheritMainConfigurations() = with(target) {
        listOfNotNull(settings.testmod, settings.gametest).forEach { cfg ->
            val ss = java.sourceSets[cfg.sourceSet.get()]
            configurations[ss.implementationConfigurationName].extendsFrom(configurations["implementation"])
            configurations[ss.compileOnlyConfigurationName].extendsFrom(configurations["compileOnly"])
            configurations[ss.runtimeOnlyConfigurationName].extendsFrom(configurations["runtimeOnly"])
        }
    }

    /**
     * Forge's gametest server needs the test data generated beforehand.
     * Runs datagen for the gametest mod and copies the output into the gametest resources output,
     * so the gametest server sees the data from the same Gradle invocation.
     */
    private fun applyGametestData(gametest: CustomSource) = with(target) {
        val sourceSet = java.sourceSets[gametest.sourceSet.get()]
        val outputDir = layout.buildDirectory.dir("generated/${sourceSet.name}Data")

        // Nested on the gametest source set -> FG registers it as `run<SourceSet>Data`.
        // The base run `data` matches the runs.json key, so FG inherits mainClass/args itself.
        minecraft.runs.maybeCreate("data").with(sourceSet.name) {
            workingDir.set(ext.buildRunDirectory(target, "${sourceSet.name}Data"))
            args("--mod", gametest.modId.get(), "--all", "--output", outputDir.get().asFile.absolutePath)
        }

        val dataTask = sourceSet.getTaskName("run", "data")               // runGametestData
        val serverTask = sourceSet.getTaskName("run", "gameTestServer")   // runGametestGameTestServer

        // processGametestResources runs before dataTask (dataTask needs it on its classpath),
        // so the fresh output has to be copied in afterward.
        val copy = tasks.register<Copy>(sourceSet.getTaskName("copy", "generatedData")) {
            dependsOn(dataTask)
            from(outputDir) { exclude(".cache/**") }
            into(provider { sourceSet.output.resourcesDir!! })
        }

        // FG registers its run tasks later (afterEvaluate -> finish), so configure lazily.
        tasks.matching { it.name == serverTask }.configureEach { dependsOn(copy) }
    }
}