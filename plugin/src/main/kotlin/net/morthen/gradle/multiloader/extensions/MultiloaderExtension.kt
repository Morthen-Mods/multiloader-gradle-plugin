package net.morthen.gradle.multiloader.extensions

import net.morthen.gradle.multiloader.api.Loader
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/** Per project configuration via `multiloader { }`. Defaults come from [MultiloaderSettingsExtension]. */
abstract class MultiloaderExtension {
    abstract var settings: MultiloaderSettingsExtension

    abstract val loader: Property<Loader>
    abstract val mixins: ListProperty<String> // Only needed for Forge

    init {
        mixins.convention(listOf())
    }
}
