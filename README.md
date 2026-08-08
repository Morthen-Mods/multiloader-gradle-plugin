![Maven metadata URL](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Fmaven.morthen.net%2Freleases%2Fnet%2Fmorthen%2Fgradle%2Fmultiloader%2Fnet.morthen.gradle.multiloader.gradle.plugin%2Fmaven-metadata.xml)
# Multiloader Gradle Plugin
A Gradle plugin to simplify Development for Minecraft Mod development.

This plugin is inspired by [UpCraft](https://github.com/Up-Mods) - [Multiloader Plugin](https://github.com/Up-Mods/multiloader-gradle-plugin), it also includes some parts of his code.

## Usage
The plugin utilizes the `multiloader` block to pre-configure each loader.

Full settings for the multiloader extension:
```kts
plugins {
    id("net.morthen.gradle.multiloader")
}

multiloader {
    // Basic settings
    javaVersion = 25            // defaults to 25
    commonRunDirectory = false  // places loader runs into the common project, active by default
    
    // Minecraft settings
    minecraftVersion = "26.1.2" // Not needed since it is loaded from the gradle.properties
    
    // Loader settings
    loader = "fabric"           // also available common(default) | datagen | forge | neoforge
    // Common
    neoFormVersion = "26.1.2-1" // only needs to be set inside the corresponding project
    // Fabric
    fabricApiVersion = "0.155.2+26.1.2" // only needs to be set inside the corresponding project
    fabricLoaderVersion = "0.19.3"      // only needs to be set inside the corresponding project
    // Forge
    forgeVersion = "64.1.0"     // only needs to be set inside the corresponding project
    forgeMixins = listOf(       // needed since forge registeres moxins though the run config and later through the manifest file
        "${ modId.get() }.mixins.json",
        "${ modId.get() }.forge.mixins.json"
    )
    // Neoforge and Datagen
    neoForgeVersion = "26.1.2.71"

    // Mod related Settings
      // All these settings gets loaded through the gradle.properties and dont have to be set
    modId = "mod_id"
    modName = "mod_name"
    modAuthor = "mod_author"
    modLicense = "mod_license"
    modDescription = "mod_description"

    // Adds the testmod sourceSet with common project dependency
    withTestMod()
    // Or
    withTestMod {
        // Decide which run should be added
        clientRun = true 
        serverRun = false
        modId = "<mod_id>" // defaults to <mod_id>_testmod
        sourceSetName = "<source_set_name>" // defaults to testmod
    }
    
    // Adds the gametest sourceSet and configures the run configurations
    withGametest()
    // Or
    withGametest {
        modId = "<mod_id>" // defaults to <mod_id>_gametest
        sourceSetName = "<source_set_name>" // defaults to gametest
    }

    // define keys that should be replaced inside the configured files
    // define loader specific replacements and the corresponding files, has to be for each loader seperately
    // These are examples and don't have to be used this way, you can name your keys as you like
    applyMetadataReplacements(listOf("pack.mcmeta", "*.mixins.json", "fabric.mod.json", "META-INF/mods.toml", "META-INF/neoforge.mods.toml"), mapOf(
        "fabric_api" to fabricApiVersion.get(),
        "fabric_loader" to fabricLoaderVersion.get(),
        
        "forge_version" to forgeVersion.get(),

        "neoforge_version" to neoForgeVersion.get()
    ))
}
```

<details>
<summary>Full Project Example</summary>

#### [Multiloader Template](https://github.com/Morthen-Mods/Multiloader-Template)


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
rootProject.name = "project-name"

listOf("common", "fabric", "datagen", "forge", "neoforge").forEach(::include)
```

#### `root/build.gradle.kts`:
```kts
plugins {
    id("java")
    id("net.morthen.gradle.multiloader") version "0.0.5" apply false
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT" apply false
    id("net.minecraftforge.gradle") version "[7.0.30, 8)" apply false
    id("net.neoforged.moddev") version "2.0.141" apply false
}
```

#### `common/build.gradle.kts`:
```kts
plugins {
    id("net.morthen.gradle.multiloader")
}

multiloader {
    neoFormVersion = "26.1.2-1"

    withTestMod()
    withGametest()
}
```

#### `datagen/build.gradle.kts`:
Utilizes the neoforge api, since that seems to be the best pick for that.
Output is: `common/src/main/generated`
```kts
plugins {
    id("net.morthen.gradle.multiloader")
}

multiloader {
    neoForgeVersion = "26.1.2.71"
    loader = "datagen"
}
```

#### `fabric/build.gradle.kts`:
```kts
plugins {
    id("net.morthen.gradle.multiloader")
}

multiloader {
    loader = "fabric"

    fabricApiVersion = "0.155.2+26.1.2"
    fabricLoaderVersion = "0.19.3"

    withTestMod()
    withGametest()

    applyMetadataReplacements(listOf("pack.mcmeta", "*.mixins.json", "fabric.mod.json"), mapOf(
        "fabric_api" to fabricApiVersion.get(),
        "fabric_loader" to fabricLoaderVersion.get()
    ))
}
```

#### `forge/build.gradle.kts`:
When Gametests are enabled Forge registeres the `runData` task since it needs to generate the `test_instance` json files.
This means you have to implement the generation you self or copy the generation from the example.
```kts
plugins {
    id("net.morthen.gradle.multiloader")
}

multiloader {
    loader = "forge"

    forgeVersion = "64.1.0"
    forgeMixins = listOf(
        "${ modId.get() }.mixins.json",
        "${ modId.get() }.forge.mixins.json"
    )

    withTestMod()
    withGametest()

    applyMetadataReplacements(listOf("pack.mcmeta", "META-INF/mods.toml"), mapOf(
        "forge_version" to forgeVersion.get()
    ))
}
```

#### `neoforge/build.gradle.kts`:
```kts
plugins {
    id("net.morthen.gradle.multiloader")
}

multiloader {
    neoForgeVersion = "26.1.2.71"
    loader = "neoforge"

    withTestMod()
    withGametest()

    applyMetadataReplacements(listOf("pack.mcmeta", "META-INF/neoforge.mods.toml"), mapOf(
        "neoforge_version" to neoForgeVersion.get()
    ))
}
```
</details>

<details>
<summary>Full Project Tree</summary>

# Project File Tree

```
multiloader-mod/
├── .gitignore
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/
│   └── wrapper/
│       ├── gradle-wrapper.jar
│       └── gradle-wrapper.properties
├── .idea/
│   ├── .gitignore
│   ├── gradle.xml
│   ├── misc.xml
│   └── vcs.xml
│
├── common/
│   ├── build.gradle.kts
│   └── src/
│       ├── gametest/
│       │   ├── java/net/morthen/example/
│       │   │   └── GametestConstants.java
│       │   └── resources/
│       │       └── pack.mcmeta
│       ├── main/
│       │   ├── java/net/morthen/example/
│       │   │   └── CommonConstants.java
│       │   └── resources/
│       │       ├── META-INF/
│       │       │   └── accesstransformer.cfg
│       │       ├── example.classtweaker
│       │       ├── example.mixins.json
│       │       └── pack.mcmeta
│       └── testmod/
│           ├── java/net/morthen/example/
│           │   └── TestConstants.java
│           └── resources/
│               └── pack.mcmeta
│
├── datagen/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── java/net/morthen/example/
│       │   ├── Example.java
│       └── resources/META-INF/
│           └── neoforge.mods.toml
│
├── fabric/
│   ├── build.gradle.kts
│   └── src/
│       ├── gametest/
│       │   ├── java/net/morthen/example/
│       │   │   └── FabricGametests.java
│       │   └── resources/
│       │       └── fabric.mod.json
│       ├── main/
│       │   ├── java/net/morthen/example/
│       │   │   └── ExampleMod.java
│       │   └── resources/
│       │       ├── example.fabric.mixins.json
│       │       └── fabric.mod.json
│       └── testmod/
│           ├── java/net/morthen/example/
│           │   └── TestMod.java
│           └── resources/
│               └── fabric.mod.json
│
├── forge/
│   ├── build.gradle.kts
│   └── src/
│       ├── gametest/
│       │   ├── java/net/morthen/example/gametest/
│       │   │   ├── ForgeGametest.java
│       │   │   └── provider/
│       │   │       └── GametestInstanceProvider.java
│       │   └── resources/META-INF/
│       │       └── mods.toml
│       ├── main/
│       │   ├── java/net/morthen/example/
│       │   │   └── ExampleMod.java
│       │   └── resources/
│       │       ├── META-INF/
│       │       │   └── mods.toml
│       │       └── example.forge.mixins.json
│       └── testmod/
│           ├── java/net/morthen/example/test/
│           │   └── TestMod.java
│           └── resources/META-INF/
│               └── mods.toml
│
└── neoforge/
    ├── build.gradle.kts
    └── src/
        ├── gametest/
        │   ├── java/net/morthen/example/
        │   │   └── NeoforgeGametest.java
        │   └── resources/META-INF/
        │       └── neoforge.mods.toml
        ├── main/
        │   ├── java/net/morthen/example/
        │   │   └── ExampleMod.java
        │   └── resources/
        │       ├── META-INF/
        │       │   └── neoforge.mods.toml
        │       └── example.neoforge.mixins.json
        └── testmod/
            ├── java/net/morthen/example/
            │   └── NeoforgeTest.java
            └── resources/META-INF/
                └── neoforge.mods.toml
```

</details>