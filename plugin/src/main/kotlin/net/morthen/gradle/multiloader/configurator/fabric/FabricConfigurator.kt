package net.morthen.gradle.multiloader.configurator.fabric

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.morthen.gradle.multiloader.api.LoaderConfigurator
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.the

class FabricConfigurator(private val target: Project, ext: MultiloaderExtension): LoaderConfigurator(ext) {
    private lateinit var loom: LoomGradleExtensionAPI

    override fun init() {
        super.init()

        target.pluginManager.apply(FabricLoom.get(obfuscated))
        loom = target.the<LoomGradleExtensionAPI>()

        applyAccessModifier()
        applyDefaultDependencies()
        createMods()
        createRuns()
    }

    // Apply the default Dependencies for Fabric. Api and Loader have to be set.
    // TODO: Should api and loader be optional?
    private fun applyDefaultDependencies() = with(target) {
        val implement = if (obfuscated) "modImplementation" else "implementation"

        dependencies {
            "minecraft"("com.mojang:minecraft:${ mcVersion }")
            if (obfuscated) "mappings"(loom.officialMojangMappings())
            implement("net.fabricmc.fabric-api:fabric-api:${ settings.fabricApiVersion.get() }")
            implement("net.fabricmc:fabric-loader:${ settings.fabricLoaderVersion.get() }")
        }
    }

    // Applies AccessWidener or ClassTweaker file.
    // Can be placed in Common or Fabric subproject
    private fun applyAccessModifier() = with(target) {
        if (ext.modifierType.orNull != null) {
            val fileName = ext.modifierType.get().getFileName(modId)
            val prov = getFile(this, fileName)
            loom.accessWidenerPath.set(prov)
        }
    }

    private fun createRuns() = with(loom) {
        // Base config for each run, also prevents the generation of Gradle runs from loom itself,
        // those get registered through the Runs itself.
        runConfigs.configureEach {
            systemProperties.put("fabric-tag-conventions-v2.missingTagTranslationWarning", "VERBOSE")
            generateRunConfig.set(false)
        }

        // Create custom runs registered in rootProject
        settings.runs.forEach { LoomRunApplier(loom, ext).apply(target, it) }
    }

    // Creates all available mods, currently only 3 possible
    // TODO: Elaborate if more custom sources are reasonable, they should be then registered in a list
    private fun createMods() = with(loom) {
        mods {
            create(modId) { sourceSet("main") }

            settings.testmod?.let {
                create(it.modId.get()) { sourceSet(it.sourceSet.get()) }
            }
            settings.gametest?.let {
                create(it.modId.get()) { sourceSet(it.sourceSet.get()) }
            }
        }
    }
}