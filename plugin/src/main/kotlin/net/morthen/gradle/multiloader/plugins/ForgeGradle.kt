package net.morthen.gradle.multiloader.plugins

import net.minecraftforge.gradle.ForgeGradleExtension
import net.minecraftforge.gradle.MinecraftExtension
import net.minecraftforge.gradle.MinecraftExtensionForProject
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.the

object ForgeGradle {
    const val PLUGIN_ID = "net.minecraftforge.gradle"
}

fun applyForgeGradle(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(ForgeGradle.PLUGIN_ID)
    val repMC = the<MinecraftExtension>()
    val minecraft = the<MinecraftExtensionForProject>()
    val fg = the<ForgeGradleExtension>()

    val mc = ext.settings.minecraftVersion.get()
    val forge = ext.settings.forgeVersion.get()

    repositories {
        repMC.mavenizer(this)
        maven(fg.forgeMaven)
        maven(fg.minecraftLibsMaven)
        mavenCentral()
    }

    dependencies {
        "implementation"(minecraft.dependency("net.minecraftforge:forge:${ mc }-${ forge }"))
    }
}