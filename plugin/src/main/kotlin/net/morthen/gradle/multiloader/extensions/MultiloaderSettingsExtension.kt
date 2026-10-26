package net.morthen.gradle.multiloader.extensions

import net.morthen.gradle.multiloader.api.MinecraftVersion
import net.morthen.gradle.multiloader.dsl.Gametest
import net.morthen.gradle.multiloader.dsl.Testmod
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property
import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ProviderFactory
import org.gradle.kotlin.dsl.newInstance

/** Shared settings, configured once in the root project via `multiloaderSettings { }`. */
abstract class MultiloaderSettingsExtension @Inject constructor(objects: ObjectFactory, val factory: ProviderFactory, val project: Project) {
    abstract val modId: Property<String>
    abstract val minecraftVersion: Property<String>

    internal var testmod: Testmod? = null
    internal var gametest: Gametest? = null

    /** Defaults to the Java version the configured Minecraft version requires. */
    val javaVersion: Property<Int> = objects.property(Int::class.java).convention(
        minecraftVersion.map { MinecraftVersion.javaVersion(it) }
    )

    init {
        modId.convention(factory.gradleProperty("mod_id"))
        minecraftVersion.convention(factory.gradleProperty("minecraft_version"))
    }

    fun withTestmod(config: Action<Testmod>? = null) = with(project) {
        val cfg = objects.newInstance(Testmod::class, this@MultiloaderSettingsExtension)
        config?.execute(cfg)

        testmod = cfg
    }

    fun withGametest(config: Action<Gametest>? = null) = with(project) {
        val cfg = objects.newInstance(Gametest::class, this@MultiloaderSettingsExtension)
        config?.execute(cfg)

        gametest = cfg
    }
}
