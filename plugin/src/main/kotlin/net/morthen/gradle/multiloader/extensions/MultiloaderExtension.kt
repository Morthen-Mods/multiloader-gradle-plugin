package net.morthen.gradle.multiloader.extensions

import net.morthen.gradle.multiloader.api.Loader
import net.morthen.gradle.multiloader.api.ModifierType
import net.morthen.gradle.multiloader.dsl.Run
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the
import java.io.File
import javax.inject.Inject

/** Per project configuration via `multiloader { }`. Defaults come from [MultiloaderSettingsExtension]. */
abstract class MultiloaderExtension @Inject constructor(val project: Project) {
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

    private fun registerFeatureSourceSet(sourceSetName: String) = with(project) {
        val javaPlugin = the(JavaPluginExtension::class)
        val sourceSet = javaPlugin.sourceSets.register(sourceSetName) {
            compileClasspath += javaPlugin.sourceSets["main"].compileClasspath
            runtimeClasspath += javaPlugin.sourceSets["main"].runtimeClasspath
        }
        sourceSet.configure {
            javaPlugin.registerFeature(sourceSetName) { usingSourceSet(this@configure) }
            dependencies { implementationConfigurationName(javaPlugin.sourceSets["main"].output) }
        }
    }

    fun registerAdditionalSourceSets() {
        settings.gametest?.let { registerFeatureSourceSet(it.sourceSet.get()) }

        settings.testmod?.let { registerFeatureSourceSet(it.sourceSet.get()) }
    }
}
