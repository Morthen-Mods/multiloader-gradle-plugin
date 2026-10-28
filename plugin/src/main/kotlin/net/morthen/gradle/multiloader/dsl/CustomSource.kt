package net.morthen.gradle.multiloader.dsl

import net.morthen.gradle.multiloader.extensions.MultiloaderSettingsExtension
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class CustomSource @Inject constructor(ext: MultiloaderSettingsExtension, source: String) {
    abstract val modId: Property<String>
    abstract val sourceSet: Property<String>
    abstract val serverRun: Property<Boolean>
    abstract val clientRun: Property<Boolean>

    init {
        modId.convention(ext.modId.get() + "_$source")
        sourceSet.convention(source)
        serverRun.convention(true)
        clientRun.convention(true)
    }
}