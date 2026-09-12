plugins {
    id("java")
    id("idea")
    id("net.morthen.gradle.multiloader") apply false

    id("net.fabricmc.fabric-loom") version "1.17.20" apply false
    id("net.minecraftforge.gradle") version "7.0.38" apply false
    id("net.neoforged.moddev") version "2.0.146" apply false

    id("me.modmuss50.mod-publish-plugin") version "2.2.0" apply false
}

subprojects {
    pluginManager.apply("idea")
    idea.module.isDownloadSources = true
}
