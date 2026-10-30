package net.morthen.gradle.multiloader.api

import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import net.morthen.gradle.multiloader.extensions.MultiloaderSettingsExtension

abstract class LoaderPlugin(protected val ext: MultiloaderExtension) {
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
}