package net.morthen.gradle.multiloader.configurator.datagen

import net.morthen.gradle.multiloader.api.LoaderConfigurator
import net.morthen.gradle.multiloader.configurator.neoforge.ModDevGradle
import net.morthen.gradle.multiloader.configurator.neoforge.NeoforgeRunApplier
import net.morthen.gradle.multiloader.dsl.Run
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import net.neoforged.moddevgradle.dsl.DataFileCollection
import net.neoforged.moddevgradle.dsl.ModDevExtension
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the

// TODO: add Datagen type to use fabric or neo/-forge as common datagen
class DatagenConfigurator(private val target: Project, ext: MultiloaderExtension): LoaderConfigurator(ext) {
    override fun init() {
        super.init()

        if (isLegacy) initLegacy()
        else initModern()
    }

    private fun initModern() = with(target) {
        pluginManager.apply(ModDevGradle.PLUGIN_ID)

        extensions.configure<NeoForgeExtension> {
            version = ext.settings.neoforgeVersion.get()

            applyAccessTransformer(accessTransformers)
            applyInterfaceInjection(interfaceInjectionData)

            createMods(this)
            createRuns(this)
        }
    }

    // TODO: Hardcoded version, will be changed later on
    private fun initLegacy() = with(target) {
        pluginManager.apply(ModDevGradle.PLUGIN_LEGACY_ID)

        extensions.configure<LegacyForgeExtension> {
            version = "1.20.1-47.3.0"

            applyAccessTransformer(accessTransformers)

            createMods(this)
            createRuns(this)
        }
    }

    // Applies accesstransformer.cfg from the common project, the file from the project path itself get applied by default
    private fun applyAccessTransformer(collection: DataFileCollection){
        val regular = getFile(target, "accesstransformer.cfg")
        if (regular.isPresent) collection.from(regular.get().asFile.absolutePath)
    }

    // Applies interface.json from common and target project, never gets applied by default
    private fun applyInterfaceInjection(collection: DataFileCollection){
        val regular = getFile(target, "interface.json")
        if (regular.isPresent) collection.from(regular.get().asFile.absolutePath)
    }

    // Registers the mod, the data run loads it through loadedMods
    private fun createMods(moddev: ModDevExtension) = with(target) {
        moddev.mods.create(modId) { sourceSet(the<JavaPluginExtension>().sourceSets["main"]) }
    }

    // Uses the Run abstraction, so the NeoForge applier picks the run type for the Minecraft version
    // (clientData from 1.21.4 on, data before) and registers the IDE run "Datagen Common Data"
    private fun createRuns(moddev: ModDevExtension) = with(target) {
        val commonProject = settings.commonProject.get()

        val run = objects.newInstance(Run::class.java, "data")
        run.clientData {
            outputDirectory.set(file("../$commonProject/src/generated"))
            existingResources.from(file("../$commonProject/src/main/resources"))
        }
        run.displayName("Common Data")
        run.systemProperty("terminal.ansi", "true")
        run.loadedMods.add(modId)

        NeoforgeRunApplier(target, ext, moddev).apply(run)
    }
}