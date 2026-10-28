pluginManagement {
    includeBuild("../../plugin")
    repositories {
        gradlePluginPortal()
        mavenCentral()

        maven("https://maven.neoforged.net/releases")
        maven("https://maven.minecraftforge.net")
        maven("https://maven.fabricmc.net")
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

rootProject.name = "example-1-21-1"

include("common")
include("datagen")
include("fabric")
include("forge")
include("neoforge")