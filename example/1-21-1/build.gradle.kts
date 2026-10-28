plugins {
    id("java")
    id("idea")
    id("net.morthen.gradle.multiloader")

    id("net.fabricmc.fabric-loom") version "1.19.3" apply false
    id("net.minecraftforge.gradle") version "7.0.40" apply false
    id("net.neoforged.moddev") version "2.0.148" apply false

    id("me.modmuss50.mod-publish-plugin") version "2.2.1" apply false
}

multiloaderSettings {
    runs {
        register("client") {
            client()
            displayName("Client (26.1)")
        }
    }
}