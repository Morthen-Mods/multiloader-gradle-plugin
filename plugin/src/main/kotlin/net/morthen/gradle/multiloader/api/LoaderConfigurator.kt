package net.morthen.gradle.multiloader.api

import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import net.morthen.gradle.multiloader.extensions.MultiloaderSettingsExtension
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.the

abstract class LoaderConfigurator(protected val ext: MultiloaderExtension) {
    protected lateinit var settings: MultiloaderSettingsExtension
    protected lateinit var mcVersion: String
    protected lateinit var modId: String

    protected var obfuscated: Boolean = false

    open fun init() {
        settings = ext.settings
        mcVersion = settings.minecraftVersion.get()
        modId = settings.modId.get()

        obfuscated = MinecraftVersion.isObfuscated(mcVersion)
    }

    fun getFile(target: Project, fileName: String): Provider<RegularFile> = with(target) {
        provider {
            listOf(project(":${settings.commonProject.get()}"), this)
                .flatMap { it.the(JavaPluginExtension::class).sourceSets["main"].resources.srcDirs }
                .flatMap { listOf(it, it.resolve("META-INF")) }
                .map { it.resolve(fileName) }
                .firstOrNull { it.isFile }
        }.map { layout.projectDirectory.file(it.absolutePath) }
    }
}