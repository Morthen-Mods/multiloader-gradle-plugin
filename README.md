![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fmaven.morthen.net%2Freleases%2Fnet%2Fmorthen%2Fgradle%2Fmultiloader%2Fnet.morthen.gradle.multiloader.gradle.plugin%2Fmaven-metadata.xml)

# Multiloader Gradle Plugin

A Gradle plugin that sets up Minecraft multiloader mod projects. You write your mod once in a
`common` project and ship it for **Fabric**, **NeoForge** and **Forge**. The plugin applies and
configures the loader toolchains, wires the common code into every loader, and adds runs, test
source sets, metadata placeholders, Maven publishing and uploads to CurseForge and Modrinth.

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
- **Shared settings in one place.** Mod info, loader versions, runs, placeholders and publishing
  are configured once in the root project and read from `gradle.properties` by default.
- **Common code included automatically.** Every loader jar contains the code and resources of
  `common`.
- **One run DSL for all loaders**, translated to Fabric Loom, ModDevGradle or ForgeGradle and
  registered as IntelliJ IDEA run configurations.
- **Extras:** test mod and game test source sets, metadata placeholders, access transformers,
  access wideners and class tweakers, interface injection, and data generation into `common`.
- **Publishing:** Maven publications for `common` and every loader, and uploads to CurseForge and
  Modrinth with shared release settings and dependencies.
- **Minecraft 1.20.1 and newer.** Java version and toolchains are picked from the Minecraft version.

## Requirements

- Gradle 9.
- Minecraft 1.20.1 or newer, release versions only.
- Gradle running on Java 17 or newer, and on **Java 25** if you have a Fabric project (Fabric
  Loom 1.19.3 is built for Java 25).
- The loader Gradle plugins you use, declared in the root project with `apply false`:
  `net.neoforged.moddev` (always), `net.fabricmc.fabric-loom` (Fabric),
  `net.minecraftforge.gradle` (Forge).

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

    // optional: upload to CurseForge (curseforge_id) and Modrinth (modrinth_id)
    withModPublish {
        dependencies { fabric { requires("fabric-api") } }
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

The full documentation is in the **[wiki](https://github.com/Morthen-Mods/multiloader-gradle-plugin/wiki)**.

**Setup**
- [Getting Started](https://github.com/Morthen-Mods/multiloader-gradle-plugin/wiki/Getting-Started): requirements, project layout and a complete setup
- [Configuration](https://github.com/Morthen-Mods/multiloader-gradle-plugin/wiki/Configuration): every setting
- [Project Types](https://github.com/Morthen-Mods/multiloader-gradle-plugin/wiki/Project-Types): what the plugin does in each subproject

**Features**
- [Runs](https://github.com/Morthen-Mods/multiloader-gradle-plugin/wiki/Runs): the shared run DSL
- [Testmod and Gametest](https://github.com/Morthen-Mods/multiloader-gradle-plugin/wiki/Testmod-and-Gametest): extra source sets for a test mod and game tests
- [Metadata Placeholders](https://github.com/Morthen-Mods/multiloader-gradle-plugin/wiki/Metadata-Placeholders): `${mod_id}`, `${version}` & co. in metadata files
- [Access Transformers and Wideners](https://github.com/Morthen-Mods/multiloader-gradle-plugin/wiki/Access-Transformers-and-Wideners): access transformers, wideners, class tweakers and interface injection
- [Maven Publishing](https://github.com/Morthen-Mods/multiloader-gradle-plugin/wiki/Maven-Publishing): Maven publications and repository
- [Mod Publishing](https://github.com/Morthen-Mods/multiloader-gradle-plugin/wiki/Mod-Publishing): uploads to CurseForge and Modrinth

**Reference**
- [Minecraft Versions](https://github.com/Morthen-Mods/multiloader-gradle-plugin/wiki/Minecraft-Versions): supported versions and what changes between them
- [Properties and Environment](https://github.com/Morthen-Mods/multiloader-gradle-plugin/wiki/Properties-and-Environment): every `gradle.properties` entry and environment variable
- [Tasks](https://github.com/Morthen-Mods/multiloader-gradle-plugin/wiki/Tasks): the Gradle tasks per project
- [Troubleshooting](https://github.com/Morthen-Mods/multiloader-gradle-plugin/wiki/Troubleshooting): error messages and what to do about them
