package net.morthen.gradle.multiloader.extensions

import net.morthen.gradle.multiloader.api.MinecraftVersion
import net.morthen.gradle.multiloader.dsl.CustomSource
import net.morthen.gradle.multiloader.dsl.Run
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.newInstance
import javax.inject.Inject

/** Shared settings, configured once in the root project via `multiloaderSettings { }`. */
abstract class MultiloaderSettingsExtension @Inject constructor(objects: ObjectFactory, factory: ProviderFactory, val project: Project) {

    // Basic
    abstract val minecraftVersion: Property<String>
    abstract val commonRunDirectory: Property<Boolean>
    abstract val commonProject: Property<String>

    // Mod Infos
    abstract val modId: Property<String>
    abstract val modName: Property<String>
    abstract val modAuthor: Property<String>
    abstract val modLicense: Property<String>
    abstract val modDescription: Property<String>

    // Mod Loader
    abstract val neoformVersion: Property<String>
    abstract val neoforgeVersion: Property<String>
    abstract val forgeVersion: Property<String>
    abstract val fabricApiVersion: Property<String>
    abstract val fabricLoaderVersion: Property<String>

    // Configurations / Settings
    internal var processResourcesProperties: List<Pair<List<String>, Map<String, Any>?>> = mutableListOf()
    internal var testmod: CustomSource? = null
    internal var gametest: CustomSource? = null
    //TODO: implement ModPublish configurations

    // Run Configurations
    abstract val runs: NamedDomainObjectContainer<Run>

    /** Defaults to the Java version the configured Minecraft version requires. */
    val javaVersion: Property<Int> = objects.property(Int::class.java).convention(
        minecraftVersion.map { MinecraftVersion.javaVersion(it) }
    )

    init {
        // Basic
        minecraftVersion.convention(factory.gradleProperty("minecraft_version"))
        commonRunDirectory.convention(false)
        commonProject.convention("common")

        // ModInfos
        modId.convention(factory.gradleProperty("mod_id"))
        modName.convention(factory.gradleProperty("mod_name"))
        modAuthor.convention(factory.gradleProperty("mod_author"))
        modLicense.convention(factory.gradleProperty("mod_license"))
        modDescription.convention(factory.gradleProperty("mod_description"))

        // Mod Loader
        neoformVersion.convention(factory.gradleProperty("neoform"))
        neoforgeVersion.convention(factory.gradleProperty("neoforge"))
        forgeVersion.convention(factory.gradleProperty("forge"))
        fabricLoaderVersion.convention(factory.gradleProperty("fabric_loader"))
        fabricApiVersion.convention(factory.gradleProperty("fabric_api"))
    }

    fun applyMetadataReplacements(pattern: List<String>, properties: Map<String, Any>? = null) {
        processResourcesProperties += pattern to properties;
    }

    fun withTestmod(config: Action<CustomSource>? = null) = with(project) {
        val source = objects.newInstance(CustomSource::class, this@MultiloaderSettingsExtension, "testmod")
            .also { config?.execute(it) }
        testmod = source

        if (source.clientRun.get()) {
            runs.register(source.sourceSet.get() + "Client") {
                client()
                displayName("Testmod Client")
                sourceSetName.set(source.sourceSet.get())
                loadedMods.addAll(modId.get(), source.modId.get())
            }
        }

        if (source.serverRun.get()) {
            runs.register(source.sourceSet.get() + "Server") {
                server()
                displayName("Testmod Server")
                sourceSetName.set(source.sourceSet.get())
                loadedMods.addAll(modId.get(), source.modId.get())
            }
        }
    }

    fun withGametest(config: Action<CustomSource>? = null) = with(project) {
        gametest = objects.newInstance(CustomSource::class, this@MultiloaderSettingsExtension, "gametest")
            .also { config?.execute(it) }
    }
}
