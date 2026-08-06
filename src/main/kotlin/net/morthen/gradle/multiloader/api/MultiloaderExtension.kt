package net.morthen.gradle.multiloader.api

import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import java.io.File
import javax.inject.Inject

@Suppress("unused")
abstract class MultiloaderExtension @Inject constructor(factory: ProviderFactory, val project: Project) {

    internal var processResourcesProperties: List<Pair<List<String>, Map<String, Any>?>> = mutableListOf()

    // Basic stuff
    abstract val javaVersion: Property<Int>
    abstract val commonRunDirectory: Property<Boolean>

    // Minecraft stuff
    abstract val loader: Property<String>
    abstract val minecraftVersion: Property<String>

    // Api stuff
    abstract val neoFormVersion: Property<String>
    abstract val fabricLoaderVersion: Property<String>
    abstract val fabricApiVersion: Property<String>
    abstract val neoForgeVersion: Property<String>
    abstract val forgeVersion: Property<String>

    // Mod stuff
    abstract val modId: Property<String>
    abstract val modName: Property<String>
    abstract val modAuthor: Property<String>
    abstract val modLicense: Property<String>
    abstract val modDescription: Property<String>

    init {
        // Basic stuff
        javaVersion.convention(25)
        commonRunDirectory.convention(true)

        // Minecraft stuff
        loader.convention("common")
        minecraftVersion.convention(factory.gradleProperty("minecraft_version"))

        // Mod stuff
        modId.convention(factory.gradleProperty("mod_id"))
        modName.convention(factory.gradleProperty("mod_name"))
        modAuthor.convention(factory.gradleProperty("mod_author"))
        modLicense.convention(factory.gradleProperty("mod_license")).orElse("MIT")
        modDescription.convention(factory.gradleProperty("mod_description")).orElse("")
    }

    fun applyMetadataReplacements(pattern: List<String>, properties: Map<String, Any>? = null) {
        processResourcesProperties += pattern to properties;
    }

    fun runDir(name: String): File {
        val baseDir = if (commonRunDirectory.get()) "../common" else project.projectDir
        return File("${baseDir}/runs/${name}")
    }
}
