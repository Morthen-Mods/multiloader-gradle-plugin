![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fmaven.morthen.net%2Freleases%2Fnet%2Fmorthen%2Fgradle%2Fmultiloader%2Fnet.morthen.gradle.multiloader.gradle.plugin%2Fmaven-metadata.xml)
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
    loader = "fabric" // also available common(default) | datagen | forge | neoforge

    // When not set only minecraft source is implemented
    fabricApiVersion = "0.155.2+26.1.2"
    fabricLoaderVersion = "0.19.3"

    // Adds the testmod sourceSet with common project dependency
    withTestMod()
    // Or
    withTestMod {
        // Decide which run should be added
        clientRun = false 
        serverRun = false
    }
    
    // Adds the gametest sourceSet and configures the run configurations
    withGametestMod()

    // define keys that should be replaced inside the configured files
    applyMetadataReplacements(listOf("pack.mcmeta", "*.mixins.json", "fabric.mod.json"), mapOf(
        "fabric_api" to fabricApiVersion.get(),
        "fabric_loader" to fabricLoaderVersion.get()
    ))
}
```

<details>
<summary>Full Project Example</summary>
#### `settings.gradle.kts`:

```kts
pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://maven.morthen.net/releases")
        maven("https://maven.neoforged.net/releases")
        maven("https://maven.minecraftforge.net")
        maven("https://maven.fabricmc.net")
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
rootProject.name = "mlp-test"
listOf("common", "fabric", "datagen", "forge", "neoforge").forEach {
    include(it)
    project(":$it").name = it
}
```
</details>