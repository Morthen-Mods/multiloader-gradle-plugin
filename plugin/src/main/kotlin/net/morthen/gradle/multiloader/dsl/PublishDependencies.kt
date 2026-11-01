package net.morthen.gradle.multiloader.dsl

import me.modmuss50.mpp.PlatformDependency.DependencyType
import net.morthen.gradle.multiloader.api.Loader
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

// TODO: Should also accept the project id?
// Base design for ModPublish Abstraction was proposed by Claude Opus 5.5 implementation made by hand
abstract class PublishDependency @Inject constructor(slug: String, val type: DependencyType) {
    abstract val curseforgeSlug: Property<String>   // Default: slug
    abstract val modrinthSlug: Property<String>     // Default: slug

    internal var loader: Loader? = null             // set by the loader block, null = every loader
    internal var onCurseforge = true
    internal var onModrinth = true

    init {
        curseforgeSlug.convention(slug)
        modrinthSlug.convention(slug)
    }

    fun curseforgeOnly() { onModrinth = false }
    fun modrinthOnly() { onCurseforge = false }
}

/** Set Dependencies for all loader with the option to apply certain dependencies for specific loader */
abstract class PublishDependencies @Inject constructor(private val objects: ObjectFactory) {
    internal val entries = mutableListOf<PublishDependency>()
    private var scope: Loader? = null

    fun requires(vararg slugs: String) = slugs.forEach { add(it, DependencyType.REQUIRED, null) }
    fun requires(slug: String, action: Action<PublishDependency>) = add(slug, DependencyType.REQUIRED, action)

    fun optional(vararg slugs: String) = slugs.forEach { add(it, DependencyType.OPTIONAL, null) }
    fun optional(slug: String, action: Action<PublishDependency>) = add(slug, DependencyType.OPTIONAL, action)

    fun incompatible(vararg slugs: String) = slugs.forEach { add(it, DependencyType.INCOMPATIBLE, null) }
    fun incompatible(slug: String, action: Action<PublishDependency>) = add(slug, DependencyType.INCOMPATIBLE, action)

    fun embeds(vararg slugs: String) = slugs.forEach { add(it, DependencyType.EMBEDDED, null) }
    fun embeds(slug: String, action: Action<PublishDependency>) = add(slug, DependencyType.EMBEDDED, action)

    // Loader scopes, everything inside only applies to that loader
    fun fabric(action: Action<PublishDependencies>) = scoped(Loader.FABRIC, action)
    fun neoforge(action: Action<PublishDependencies>) = scoped(Loader.NEOFORGE, action)
    fun forge(action: Action<PublishDependencies>) = scoped(Loader.FORGE, action)

    internal fun forLoader(loader: Loader): List<PublishDependency> =
        entries.filter { it.loader == null || it.loader == loader }

    private fun scoped(loader: Loader, action: Action<PublishDependencies>) {
        if (scope != null) throw GradleException("Loader blocks can't be nested: ${loader.name.lowercase()} { } inside ${scope!!.name.lowercase()} { }")
        val child = objects.newInstance(PublishDependencies::class.java)
        child.scope = loader
        action.execute(child)
        entries += child.entries
    }

    private fun add(slug: String, type: DependencyType, action: Action<PublishDependency>?) {
        val dependency = objects.newInstance(PublishDependency::class.java, slug, type)
        dependency.loader = scope
        action?.execute(dependency)
        entries += dependency
    }
}