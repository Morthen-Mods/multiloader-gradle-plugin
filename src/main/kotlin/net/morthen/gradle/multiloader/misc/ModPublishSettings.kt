package net.morthen.gradle.multiloader.misc

import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.ReleaseType
import me.modmuss50.mpp.platforms.modrinth.ModrinthEnvironment
import net.morthen.gradle.multiloader.api.MultiloaderExtension
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.the

fun applyModPublishSettings(current: Project, ext: MultiloaderExtension) = with(current) {
    afterEvaluate {
        val config = ext.modPublishConfig ?: return@afterEvaluate

        pluginManager.apply("me.modmuss50.mod-publish-plugin")

        the<ModPublishExtension>().apply {
            file.set(tasks.named<Jar>("jar").flatMap { it.archiveFile })
            displayName.set(file.get().asFile.name)
            type.set(ReleaseType.of(config.versionType.get()))
            modLoaders.add(ext.loader.get())

            val changelogFile = rootProject.file(config.changelogFile.get())
            changelog.set(if (changelogFile.exists()) changelogFile.readText() else "No changelog provided.")

            if (config.curseforgeId.isPresent && config.curseforgeApi.isPresent) {
                curseforge {
                    projectId.set(config.curseforgeId.get())
                    accessToken.set(config.curseforgeApi.get())
                    minecraftVersions.add(ext.minecraftVersion.get())

                    client.set(config.environment.get().client)
                    server.set(config.environment.get().server)

                    config.required.get().forEach { requires(it) }
                    config.optional.get().forEach { optional(it) }
                    config.incompatible.get().forEach { incompatible(it) }
                    config.embedded.get().forEach { embeds(it) }
                }
            }

            if (config.modrinthId.isPresent && config.modrinthApi.isPresent) {
                modrinth {
                    projectId.set(config.modrinthId.get())
                    accessToken.set(config.modrinthApi.get())
                    minecraftVersions.add(ext.minecraftVersion.get())

                    environment.set(config.environment.get().getModrinthEnv())

                    config.required.get().forEach { requires(it) }
                    config.optional.get().forEach { optional(it) }
                    config.incompatible.get().forEach { incompatible(it) }
                    config.embedded.get().forEach { embeds(it) }
                }
            }
        }
    }
}
