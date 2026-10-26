plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
    `version-catalog`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.gradle.publishing)
}

base.archivesName = "multiloader-gradle-plugin"

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
    implementation(libs.mod.publish)

    compileOnly(libs.moddevgradle)
    compileOnly(libs.fabric.loom)
    compileOnly(libs.fabric.loom.remap)
    compileOnly(libs.forge.gradle)
}

// Build with JDK 25, but emit Java 17 bytecode so the plugin runs on Gradle with JVM 17+
java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 17
}

kotlin {
    compilerOptions.jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
}

// The compileOnly plugin dependencies are built for newer JVMs; they are only read at compile time and never shipped
configurations.compileClasspath {
    attributes.attribute(TargetJvmVersion.TARGET_JVM_VERSION_ATTRIBUTE, 25)
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