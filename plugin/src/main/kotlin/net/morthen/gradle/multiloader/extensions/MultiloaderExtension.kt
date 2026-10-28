package net.morthen.gradle.multiloader.extensions

import net.morthen.gradle.multiloader.api.Loader
import net.morthen.gradle.multiloader.api.ModifierType
import net.morthen.gradle.multiloader.dsl.Run
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import java.io.File

/** Per project configuration via `multiloader { }`. Defaults come from [MultiloaderSettingsExtension]. */
abstract class MultiloaderExtension {
    abstract var settings: MultiloaderSettingsExtension

    abstract val loader: Property<Loader>
    abstract val mixins: ListProperty<String> // Only needed for Forge
    abstract val modifierType: Property<ModifierType> // Only needed for Fabric

    init {
        mixins.convention(listOf())
    }

    fun buildRunDirectory(target: Project, run: Run): File {
        val common = settings.commonRunDirectory.get()
        val baseDir = if (common) "../common" else target.projectDir
        return File("$baseDir/runs/${run.runDirectory.orElse(run.name).get()}")
    }
}
