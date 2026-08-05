plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    `version-catalog`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gradle.publishing)
}

repositories {
    mavenCentral()
    gradlePluginPortal()

    maven("https://maven.neoforged.net/releases") {
        name = "NeoForge"
    }
    maven("https://maven.fabricmc.net") {
        name = "FabricMC"
    }
    maven("https://maven.minecraftforge.net") {
        name = "MinecraftForge"
    }
}

dependencies {
    compileOnly(gradleApi())
    compileOnly(gradleKotlinDsl())

    implementation(libs.idea.ext)
    compileOnly(libs.moddevgradle)
    compileOnly(libs.fabric.loom)
    compileOnly(libs.forge.gradle)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.named<Jar>("jar").configure {
    manifest.attributes(
        "Implementation-Title" to project.name,
        "Implementation-Version" to project.version,
        "Built-On-Java" to "${providers.systemProperty("java.vm.version").orNull} (${providers.systemProperty("java.vm.vendor").orNull})"
    )
}

gradlePlugin {
    website = "https://maven.sbvl.net"
    vcsUrl = "https://github.com/Stein-N/multiloader-gradle-plugin"

    plugins.create("multiloader") {
        id = "net.morthen.gradle.multiloader"
        implementationClass = "net.morthen.gradle.multiloader.MultiloaderPlugin"
        displayName = "Minecraft Multiloader Plugin"
        description = "Common Configuration plugin for Minecraft Multiloader Mods, inspired by the work of UpCraft"
        tags.addAll("minecraft", "multiloader", "mods")
    }
}

publishing {
    repositories {
        providers.environmentVariable("MAVEN_URL").orNull?.let {
            maven(it) {
                credentials {
                    username = System.getenv("MAVEN_USER")
                    password = System.getenv("MAVEN_PASSWORD")
                }
            }
        }
    }
}