package net.morthen.gradle.multiloader.plugins.fabric

object FabricLoom {
    const val PLUGIN_ID = "net.fabricmc.fabric-loom"
    const val PLUGIN_REMAP_ID = "net.fabricmc.fabric-loom-remap"

    fun get(obfuscated: Boolean): String {
        return if (obfuscated) PLUGIN_REMAP_ID else PLUGIN_ID
    }
}