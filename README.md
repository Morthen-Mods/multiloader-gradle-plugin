[](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fmaven.morthen.net%2Freleases%2Fnet%2Fmorthen%2Fgradle%2Fmultiloader%2Fnet.morthen.gradle.multiloader.gradle.plugin%2Fmaven-metadata.xml)

# Multiloader Gradle Plugin
A Gradle plugin to simplify Development for Minecraft Mod development.

This plugin is inspired by [UpCraft](https://github.com/Up-Mods) - [Multiloader Plugin](https://github.com/Up-Mods/multiloader-gradle-plugin),
it also includes some of his Code so in conclusion most of the heavy work was done by him.

## Usage
The plugin utilizes the `multiloader` block to pre-configure each loader.
her an example for the Fabric loader:
```kts
plugins {
    id("net.morthen.gradle.multiloader")
}

multiloader {
    loader = "fabric"

    fabricApiVersion = "0.155.2+26.1.2"
    fabricLoaderVersion = "0.19.3"

    withTestMod()
    withGametestMod()

    applyMetadataReplacements(listOf("pack.mcmeta", "*.mixins.json", "fabric.mod.json"), mapOf(
        "fabric_api" to fabricApiVersion.get(),
        "fabric_loader" to fabricLoaderVersion.get()
    ))
}
```