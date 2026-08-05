package net.morthen.gradle.multiloader.plugins

import net.morthen.gradle.multiloader.api.MultiloaderExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withGroovyBuilder

object ForgeGradle {
    const val PLUGIN_ID = "net.minecraftforge.gradle"
}

// ForgeGradle's `minecraft { }` DSL is Groovy-based and its extension type isn't a stable
// compile-time dependency here, so it's configured dynamically via withGroovyBuilder.
fun applyForgeGradle(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(ForgeGradle.PLUGIN_ID)

    dependencies {
        "minecraft"("net.minecraftforge:forge:${ext.minecraftVersion.get()}-${ext.forgeVersion.get()}")
    }
}
