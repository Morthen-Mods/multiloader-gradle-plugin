package net.morthen.gradle.multiloader.conventions

import me.modmuss50.mpp.ModPublishExtension
import me.modmuss50.mpp.PlatformDependency
import me.modmuss50.mpp.PlatformDependency.DependencyType
import me.modmuss50.mpp.PlatformDependencyContainer
import net.morthen.gradle.multiloader.api.Loader
import net.morthen.gradle.multiloader.extensions.MultiloaderSettingsExtension
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.named

private const val MOD_PUBLISH_PLUGIN_ID = "me.modmuss50.mod-publish-plugin"

// Base design for ModPublish Abstraction was proposed by Claude Opus 5.5 implementation made by hand
internal fun Project.applyModPublish(settings: MultiloaderSettingsExtension, loader: Loader, jarTask: String) {
    val spec = settings.modPublish ?: return
    if (spec.loaders.get().isNotEmpty() && loader !in spec.loaders.get()) return

    // A platform is enabled by its projectId (gradle.properties or the platform block)
    val cf = spec.curseforge
    val mr = spec.modrinth
    if (!cf.projectId.isPresent && !mr.projectId.isPresent) {
        logger.error("withModPublish is configured, but neither curseforge_id nor modrinth_id is set. Nothing gets published for $path")
        return
    }

    pluginManager.apply(MOD_PUBLISH_PLUGIN_ID)

    // Resolved here, inside publishMods { } `version` would be the extension property
    val projectVersion = version.toString()
    val mcVersions = (listOf(settings.minecraftVersion.get()) + spec.additionalMinecraftVersions.get()).distinct()
    val side = spec.side.get()
    val publishDeps = spec.dependencies.forLoader(loader)
    val rootChangelog = providers.fileContents(rootProject.layout.projectDirectory.file("CHANGELOG.md")).asText

    extensions.configure<ModPublishExtension> {
        // Shared values, every platform inherits them (ModPublishExtension -> platform.from(this))
        file.convention(tasks.named<AbstractArchiveTask>(jarTask).flatMap { it.archiveFile })
        version.convention(projectVersion)
        displayName.convention(file.get().asFile.name)
        changelog.convention(spec.changelog.orElse(rootChangelog).orElse(""))
        type.convention(spec.type)
        modLoaders.convention(listOf(loader.getId()))
        dryRun.convention(spec.dryRun)

        if (cf.projectId.isPresent) curseforge {
            projectId.convention(cf.projectId)
            accessToken.convention(cf.accessToken)
            minecraftVersions.convention(mcVersions)
            javaVersions.convention(settings.javaVersion.map { listOf(JavaVersion.toVersion(it)) })
            client.convention(side.curseforgeClient)
            server.convention(side.curseforgeServer)

            publishDeps.filter { it.onCurseforge }.forEach { dep ->
                addDependency(dep.type) { slug.set(dep.curseforgeSlug) }
            }
        }

        if (mr.projectId.isPresent) modrinth {
            projectId.convention(mr.projectId)
            accessToken.convention(mr.accessToken)
            featured.convention(mr.featured)
            minecraftVersions.convention(mcVersions)
            environment.convention(side.modrinth)

            publishDeps.filter { it.onModrinth }.forEach { dep ->
                addDependency(dep.type) { slug.set(dep.modrinthSlug) }
            }
        }
    }
}

// Maps our dependency type onto the public requires/optional/... functions of mod-publish-plugin
private fun <T : PlatformDependency> PlatformDependencyContainer<T>.addDependency(type: DependencyType, action: Action<T>) =
    when (type) {
        DependencyType.REQUIRED -> requires(action)
        DependencyType.OPTIONAL -> optional(action)
        DependencyType.INCOMPATIBLE -> incompatible(action)
        DependencyType.EMBEDDED -> embeds(action)
    }