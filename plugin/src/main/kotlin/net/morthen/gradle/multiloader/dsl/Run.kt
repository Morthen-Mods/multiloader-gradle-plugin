package net.morthen.gradle.multiloader.dsl

import net.morthen.gradle.multiloader.api.Loader
import org.gradle.api.Action
import org.gradle.api.Named
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

enum class RunType(val client: Boolean) {
    CLIENT(true),
    SERVER(false),
    CLIENT_DATA(true),
    SERVER_DATA(false),
    GAMETEST_SERVER(false),
    CLIENT_GAMETEST(true);   // Only fabric supports Client Gametests
}

abstract class DatagenSpec {
    abstract val outputDirectory: DirectoryProperty
    abstract val existingResources: ConfigurableFileCollection // only Forge/NeoForge (--existing)
    abstract val strictValidation: Property<Boolean>           // only Fabric
}

/**
 * Base design for Run Abstraction was proposed by Claude Opus 5.5 implementation made by hand
 */
abstract class Run @Inject constructor(private val name: String, objects: ObjectFactory): Named {
    override fun getName() = name

    // Base run information
    abstract val type: Property<RunType>
    abstract val loaders: SetProperty<Loader>
    abstract val sourceSetName: Property<String>
    abstract val loadedMods: SetProperty<String>

    // Run configuration
    abstract val displayName: Property<String>
    abstract val mainClass: Property<String>
    abstract val runDirectory: Property<String>
    abstract val programArgs: ListProperty<String>
    abstract val jvmArgs: ListProperty<String>
    abstract val systemProperties: MapProperty<String, String>
    abstract val environment: MapProperty<String, String>

    // Convenience settings
    abstract val gui: Property<Boolean>         // Is basically always false
    abstract val maxHeapSize: Property<String>

    val datagen: DatagenSpec = objects.newInstance(DatagenSpec::class.java)

    init {
        sourceSetName.convention("main")
        gui.convention(false)
    }

    // Runtype convenience functions
    fun client() = type.set(RunType.CLIENT)
    fun server() = type.set(RunType.SERVER) // nogui parameter will be set through the corresponding applier
    fun clientData(action: Action<DatagenSpec>? = null) {
        type.set(RunType.CLIENT_DATA)
        action?.execute(datagen)
    }
    fun serverData(action: Action<DatagenSpec>? = null) {
        type.set(RunType.SERVER_DATA)
        action?.execute(datagen)
    }
    fun gameTestServer() = type.set(RunType.GAMETEST_SERVER)

    // register a run for specific loader
    fun onlyOn(vararg loader: Loader) = loaders.addAll(*loader)

    fun runDirectory(name: String) = runDirectory.set(name)
    fun displayName(name: String) = displayName.set(name)

    fun programArg(arg: String) = programArgs.add(arg)
    fun programArgs(vararg args: String) = programArgs.addAll(*args)
    fun jvmArg(arg: String) = jvmArgs.add(arg)
    fun jvmArgs(vararg args: String) = jvmArgs.addAll(*args)
    fun systemProperty(key: String, value: String) = systemProperties.put(key, value)
    fun environment(key: String, value: String) = environment.put(key, value)
}
