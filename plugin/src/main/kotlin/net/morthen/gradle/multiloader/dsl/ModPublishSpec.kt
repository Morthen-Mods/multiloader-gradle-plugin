package net.morthen.gradle.multiloader.dsl

import me.modmuss50.mpp.ReleaseType
import net.morthen.gradle.multiloader.api.Loader
import net.morthen.gradle.multiloader.api.ModSide
import org.gradle.api.Action
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.ProviderFactory
import org.gradle.api.provider.SetProperty
import javax.inject.Inject

// Base design for ModPublish Abstraction was proposed by Claude Opus 5.5 implementation made by hand
abstract class ModPublishSpec @Inject constructor(objects: ObjectFactory, providers: ProviderFactory) {
    // Shortcuts, so the root build script doesn't need to import ReleaseType
    val STABLE = ReleaseType.STABLE
    val BETA = ReleaseType.BETA
    val ALPHA = ReleaseType.ALPHA

    // Shared by every platform and loader
    abstract val type: Property<ReleaseType>
    abstract val changelog: Property<String> // Defaults to CHANGELOG.md from the root of the project
    abstract val side: Property<ModSide>
    abstract val additionalMinecraftVersions: ListProperty<String>
    abstract val loaders: SetProperty<Loader>
    abstract val dryRun: Property<Boolean>

    // Platform is enabled as soon as its projectId is set.
    val curseforge: CurseforgeSpec = objects.newInstance(CurseforgeSpec::class.java)
    val modrinth: ModrinthSpec = objects.newInstance(ModrinthSpec::class.java)

    val dependencies: PublishDependencies = objects.newInstance(PublishDependencies::class.java)

    init {
        type .convention(STABLE)
        side.convention(ModSide.CLIENT_AND_SERVER)
        dryRun.convention(providers.gradleProperty("mod_publish_dry_run").map(String::toBoolean).orElse(false))
    }

    fun curseforge(action: Action<CurseforgeSpec>) = action.execute(curseforge)
    fun modrinth(action: Action<ModrinthSpec>) = action.execute(modrinth)
    fun dependencies(action: Action<PublishDependencies>) = action.execute(dependencies)

    // Convenience
    fun onlyOn(vararg loader: Loader) = loaders.addAll(*loader)
}

abstract class CurseforgeSpec @Inject constructor(providers: ProviderFactory) {
    abstract val projectId: Property<String>
    abstract val accessToken: Property<String>

    init {
        projectId.convention(providers.gradleProperty("curseforge_id"))
        accessToken.convention(providers.environmentVariable("CURSEFORGE_TOKEN"))
    }
}

abstract class ModrinthSpec @Inject constructor(providers: ProviderFactory) {
    abstract val projectId: Property<String>
    abstract val accessToken: Property<String>
    abstract val featured: Property<Boolean>

    init {
        projectId.convention(providers.gradleProperty("modrinth_id"))
        accessToken.convention(providers.environmentVariable("MODRINTH_TOKEN"))
        featured.convention(false)
    }
}