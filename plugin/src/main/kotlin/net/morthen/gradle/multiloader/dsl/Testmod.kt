package net.morthen.gradle.multiloader.dsl

import net.morthen.gradle.multiloader.extensions.MultiloaderSettingsExtension
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class Testmod @Inject constructor(ext: MultiloaderSettingsExtension) {
    abstract val modId: Property<String>
    abstract val sourceSetName: Property<String>

    init {
        modId.convention(ext.modId.map { "${ it }_testmod" })
        sourceSetName.convention("testmod")
    }
}