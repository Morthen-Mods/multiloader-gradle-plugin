package net.morthen.gradle.multiloader.dsl

import org.gradle.api.Named
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

enum class RunType { CLIENT, SERVER }

abstract class Run @Inject constructor(private val name: String): Named {
    override fun getName() = name

    // Identity
    abstract val type: Property<RunType>
    abstract val displayName: Property<String>
    abstract val mainClass: Property<String>
    abstract val sourceSetName: Property<String>

    // System and JVM Settings
    abstract val runDirectory: Property<String>
    abstract val programArgs: ListProperty<String>
    abstract val jvmArgs: ListProperty<String>
    abstract val systemProperties: MapProperty<String, String>
    abstract val environment: MapProperty<String, String>

    // Mods
    abstract val loadedMods: SetProperty<String>

    init {
        sourceSetName.convention("main")
    }

    fun client() = type.set(RunType.CLIENT)
    fun server() = type.set(RunType.SERVER)

    fun runDirectory(name: String) = runDirectory.set(name)
    fun displayName(name: String) = displayName.set(name)

    fun programArg(arg: String) = programArgs.add(arg)
    fun programArgs(vararg args: String) = programArgs.addAll(*args)
    fun jvmArg(arg: String) = jvmArgs.add(arg)
    fun jvmArgs(vararg args: String) = jvmArgs.addAll(*args)
    fun systemProperty(key: String, value: String) = systemProperties.put(key, value)
    fun environment(key: String, value: String) = environment.put(key, value)
}
