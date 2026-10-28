package net.morthen.gradle.multiloader.extensions

import net.morthen.gradle.multiloader.api.MinecraftVersion
import net.morthen.gradle.multiloader.dsl.Gametest
import net.morthen.gradle.multiloader.dsl.Run
import net.morthen.gradle.multiloader.dsl.Testmod
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.newInstance
import java.io.File
import javax.inject.Inject

/** Shared settings, configured once in the root project via `multiloaderSettings { }`. */
abstract class MultiloaderSettingsExtension @Inject constructor(objects: ObjectFactory, val factory: ProviderFactory, val project: Project) {
    // Basic
    abstract val minecraftVersion: Property<String>
    abstract val commonRunDirectory: Property<Boolean>

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
    internal var testmod: Testmod? = null
    internal var gametest: Gametest? = null
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

    fun withTestmod(config: Action<Testmod>? = null) = with(project) {
        testmod = objects.newInstance(Testmod::class, this@MultiloaderSettingsExtension)
            .also { config?.execute(it) }
    }

    fun withGametest(config: Action<Gametest>? = null) = with(project) {
        gametest = objects.newInstance(Gametest::class, this@MultiloaderSettingsExtension)
            .also { config?.execute(it) }
    }
}
