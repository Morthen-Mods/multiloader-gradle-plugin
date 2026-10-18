package net.morthen.gradle.multiloader.extensions

import net.morthen.gradle.multiloader.api.MinecraftVersion
import org.gradle.api.provider.Property
import javax.inject.Inject
import org.gradle.api.model.ObjectFactory

/** Shared settings, configured once in the root project via `multiloaderSettings { }`. */
abstract class MultiloaderSettingsExtension @Inject constructor(objects: ObjectFactory) {
    abstract val modId: Property<String>
    abstract val minecraftVersion: Property<String>

    /** Defaults to the Java version the configured Minecraft version requires. */
    val javaVersion: Property<Int> = objects.property(Int::class.java).convention(
        minecraftVersion.map { MinecraftVersion.javaVersion(it) }
    )
}
