![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fmaven.morthen.net%2Freleases%2Fnet%2Fmorthen%2Fgradle%2Fmultiloader%2Fnet.morthen.gradle.multiloader.gradle.plugin%2Fmaven-metadata.xml)

# Multiloader Gradle Plugin

A Gradle plugin that sets up Minecraft multiloader mod projects. You write your mod once in a
`common` project and ship it for **Fabric**, **NeoForge** and **Forge**. The plugin applies and
configures the loader toolchains, wires the common code into every loader, and adds runs, test
source sets, metadata placeholders and Maven publishing.

> [!IMPORTANT]
> **Credits:** This plugin is inspired by [UpCraft](https://github.com/Up-Mods)'s
> [Multiloader Plugin](https://github.com/Up-Mods/multiloader-gradle-plugin) and contains parts
> of its code.

> [!NOTE]
> This README and the wiki were written with the help of AI (Claude by Anthropic), based on the
> plugin's source code. If something here does not match the plugin's behaviour, the code is
> right. Please open an issue.

## What it does

- **One plugin for every subproject.** A single `loader` setting turns a subproject into the
  `common`, `datagen`, `fabric`, `forge` or `neoforge` project.
- **Shared settings in one place.** Mod info, loader versions and runs are configured once in the
  root project and read from `gradle.properties` by default.
- **Common code included automatically.** Every loader jar contains the code and resources of
  `common`.
- **One run DSL for all loaders**, registered as IntelliJ IDEA run configurations.
- **Extras:** test mod and game test source sets, metadata placeholders, access transformers and
  wideners, data generation into `common`, and Maven publishing.
- **Minecraft 1.20.1 and newer.** Java version and toolchains are picked from the Minecraft version.

## Quick look

`build.gradle.kts (root)`

```kts
plugins {
    id("net.morthen.gradle.multiloader") version "0.2.0"
    id("net.neoforged.moddev") version "2.0.148" apply false
    id("net.fabricmc.fabric-loom") version "1.19.3" apply false
}

multiloaderSettings {
    runs {
        register("client") { client(); loadedMods.add(modId) }
    }
}
```

`fabric/build.gradle.kts`

```kts
import net.morthen.gradle.multiloader.api.Loader

plugins {
    id("net.morthen.gradle.multiloader")
}

multiloader {
    loader = Loader.FABRIC
}
```

## Documentation

The full documentation is in the **[wiki](https://github.com/Morthen-Mods/multiloader-gradle-plugin/wiki)**:

- [Getting Started](https://github.com/Morthen-Mods/multiloader-gradle-plugin/wiki/Getting-Started): requirements, project layout and a complete setup
- [Configuration](https://github.com/Morthen-Mods/multiloader-gradle-plugin/wiki/Configuration): every setting
- [Runs](https://github.com/Morthen-Mods/multiloader-gradle-plugin/wiki/Runs): the shared run DSL
