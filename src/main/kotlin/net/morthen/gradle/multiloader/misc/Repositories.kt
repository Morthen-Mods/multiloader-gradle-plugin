package net.morthen.gradle.multiloader.misc

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories

internal fun Project.applyDefaultRepositories() {
    repositories {
        mavenCentral()
        maven("https://maven.morthen.net/releases")
        maven("https://maven.terraformersmc.com/")

        // mod loaders
        maven("https://maven.neoforged.net/releases") {
            name = "NeoForge"
        }
        maven("https://maven.fabricmc.net") {
            name = "FabricMC"
        }

        // mod platforms
        curseforge()
        modrinth()
    }
}

fun RepositoryHandler.modrinth() {
    exclusiveContent {
        forRepository {
            maven("https://api.modrinth.com/maven") {
                name = "Modrinth"
            }
        }
        filter { includeGroup("maven.modrinth") }
    }
}

fun RepositoryHandler.curseforge() {
    exclusiveContent {
        forRepository {
            maven("https://www.cursemaven.com") {
                name = "Curseforge"
            }
        }
        filter { includeGroup("curse.maven") }
    }
}