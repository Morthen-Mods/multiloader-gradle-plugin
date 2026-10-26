package net.morthen.gradle.multiloader.extensions

import net.morthen.gradle.multiloader.api.Loader
import net.morthen.gradle.multiloader.dsl.Run
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.provider.Property

/** Per project configuration via `multiloader { }`. Defaults come from [MultiloaderSettingsExtension]. */
abstract class MultiloaderExtension {
    abstract val loader: Property<Loader>
    abstract val minecraftVersion: Property<String>
    abstract val javaVersion: Property<Int>
    abstract val runs: NamedDomainObjectContainer<Run> //TODO: how do i apply those correctly
}
