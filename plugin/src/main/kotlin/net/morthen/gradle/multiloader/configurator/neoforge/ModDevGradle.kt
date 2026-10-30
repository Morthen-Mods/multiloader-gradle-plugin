package net.morthen.gradle.multiloader.configurator.neoforge

import net.morthen.gradle.multiloader.conventions.applyGradleConvention
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import net.neoforged.moddevgradle.dsl.DataFileCollection
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

object ModDevGradle {
    const val PLUGIN_ID = "net.neoforged.moddev"
    const val PLUGIN_LEGACY_ID = "net.neoforged.moddev.legacyforge"
}

fun addIfExists(collection: DataFileCollection, target: Project, relativePath: String) {
    val commonFile = target.project(":common").file(relativePath)
    val selfFile = target.file(relativePath)

    if (commonFile.exists()) collection.from(commonFile)
    if (selfFile.exists()) collection.from(selfFile)
}

fun applyDefaultTransformer(target: Project, extension: NeoForgeExtension) {
    extension.validateAccessTransformers.set(true)

    addIfExists(extension.accessTransformers, target, "src/main/resources/META-INF/accesstransformer.cfg")
    addIfExists(extension.interfaceInjectionData, target, "src/main/resources/META-INF/interface.json")
}



fun applyModDevDatagen(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(ModDevGradle.PLUGIN_ID)
    applyGradleConvention("neoforge")

    extensions.configure<NeoForgeExtension> {
        version = ext.settings.neoforgeVersion.get()
        applyDefaultTransformer(target, this)

        runs {
            register("data") {
                clientData()
                disableIdeRun()
                systemProperty("terminal.ansi", "true")
                gameDirectory.set(ext.buildRunDirectory(target, "data"))
                programArguments.set(listOf("--mod", ext.settings.modId.get(), "--all", "--output", file("../common/src/generated").absolutePath, "--existing", file("../common/src/main/resources").absolutePath))

                rootProject.the(IdeaModel::class).project.settings.runConfigurations.create<Gradle>("Common Datagen") {
                    taskNames = listOf(":${target.name}:runData")
                    setProject(target)
                }
            }
        }
    }
}