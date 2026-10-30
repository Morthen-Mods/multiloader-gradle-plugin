package net.morthen.gradle.multiloader.plugins.fabric

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.morthen.gradle.multiloader.api.LoaderPlugin
import net.morthen.gradle.multiloader.api.MinecraftVersion
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import net.morthen.gradle.multiloader.extensions.MultiloaderSettingsExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the

class FabricPlugin(private val target: Project, private val ext: MultiloaderExtension): LoaderPlugin {
    private lateinit var settings: MultiloaderSettingsExtension
    private lateinit var loom: LoomGradleExtensionAPI
    private lateinit var mcVersion: String
    private lateinit var modId: String
    private lateinit var implement: String

    private var obfuscated: Boolean = false

    override fun init() = with(target) {
        settings = ext.settings
        mcVersion = settings.minecraftVersion.get()
        modId = settings.modId.get()
        obfuscated = MinecraftVersion.isObfuscated(mcVersion)
        implement = if (obfuscated) "modImplemention" else "implementation"

        pluginManager.apply(FabricLoom.get(obfuscated))
        loom = the<LoomGradleExtensionAPI>()

        applyAccessModifier()
        applyDefaultDependencies()
        createMods()
        createRuns()
    }

    // Apply the default Dependencies for Fabric. Api and Loader have to be set.
    // TODO: Should api and loader be optional?
    private fun applyDefaultDependencies() = with(target) {
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

            loom.accessWidenerPath.convention(provider {
                // TODO: apply custom common project name properly when implemented
                listOf(this, project(":common"))
                    .flatMap { it.the(JavaPluginExtension::class).sourceSets["main"].resources.srcDirs }
                    .map { it.resolve(fileName) }
                    .firstOrNull { it.isFile }
            }.map { layout.projectDirectory.file(it.absolutePath) })
        }
    }

    private fun createRuns() = with(loom) {
        // Base config on each loader, also prevents the generation of default runs from loom itself
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