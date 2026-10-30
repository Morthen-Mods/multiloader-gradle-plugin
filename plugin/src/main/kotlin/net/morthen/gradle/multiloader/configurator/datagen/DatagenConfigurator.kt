package net.morthen.gradle.multiloader.configurator.datagen

import net.morthen.gradle.multiloader.api.LoaderPlugin
import net.morthen.gradle.multiloader.configurator.neoforge.ModDevGradle
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import net.neoforged.moddevgradle.dsl.DataFileCollection
import net.neoforged.moddevgradle.dsl.ModDevExtension
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

class DatagenConfigurator(private val target: Project, private val legacy: Boolean, ext: MultiloaderExtension): LoaderPlugin(ext) {
    override fun init() {
        super.init()

        if (legacy) initLegacy() else initModern()
    }

    private fun initModern() = with(target) {
        pluginManager.apply(ModDevGradle.PLUGIN_ID)

        extensions.configure<NeoForgeExtension> {
            neoFormVersion = settings.neoformVersion.get()

            applyAccessTransformer(accessTransformers)
            applyInterfaceInjection(interfaceInjectionData)

            createRuns(this)
        }
    }

    private fun initLegacy() = with(target) {
        pluginManager.apply(ModDevGradle.PLUGIN_LEGACY_ID)

        extensions.configure<LegacyForgeExtension> {
            mcpVersion = mcVersion

            applyAccessTransformer(accessTransformers)

            createRuns(this)
        }
    }

    // Applies accesstransformer.cfg from the common project, the file from the project path itself get applied by default
    private fun applyAccessTransformer(collection: DataFileCollection){
        target.project(":common").file("src/main/resources/META-INF/accesstransformer.cfg").let { collection.from(it.absolutePath) }
    }

    // Applies interface.json from common and target project, never gets applied by default
    private fun applyInterfaceInjection(collection: DataFileCollection){
        target.project(":common").file("src/main/resources/META-INF/interface.json").let { collection.from(it.absolutePath) }
        target.file("src/main/resources/META-INF/interface.json").let { collection.from(it.absolutePath) }
    }

    private fun createRuns(moddev: ModDevExtension) = with(target) {
        moddev.runs {
            register("data") {
                clientData()
                disableIdeRun()
                systemProperty("terminal.ansi", "true")
                gameDirectory.set(ext.buildRunDirectory(target, "data"))
                programArguments.set(listOf("--mod", ext.settings.modId.get(), "--all", "--output", file("../common/src/generated").absolutePath, "--existing", file("../common/src/main/resources").absolutePath))

                rootProject.the(IdeaModel::class).project.settings.runConfigurations.create<Gradle>("Common Datagen") {
                    taskNames = listOf(":${target.name}:runData")
                    setProject(target)
                }
            }
        }
    }
}