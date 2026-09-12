package net.morthen.gradle.multiloader.api

import me.modmuss50.mpp.platforms.modrinth.ModrinthEnvironment
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class ModPublishConfiguration @Inject constructor(project: Project) {
    enum class ModEnvironment(val client: Boolean, val server: Boolean) {
        CLIENT(true, false),
        SERVER(false, true),
        CLIENT_AND_SERVER(true, true);

        fun getModrinthEnv(): ModrinthEnvironment {
             if (name.equals("client", true)) {
                 return ModrinthEnvironment.CLIENT_ONLY
             }

            if (name.equals("server", true)) {
                return ModrinthEnvironment.SERVER_ONLY
            }

            return ModrinthEnvironment.CLIENT_AND_SERVER
        }
    }

    abstract val changelogFile: Property<String>
    abstract val versionType: Property<String>

    abstract val curseforgeId: Property<String>
    abstract val curseforgeApi: Property<String>
    abstract val modrinthId: Property<String>
    abstract val modrinthApi: Property<String>

    abstract val environment: Property<ModEnvironment>

    abstract val required: ListProperty<String>
    abstract val optional: ListProperty<String>
    abstract val incompatible: ListProperty<String>
    abstract val embedded: ListProperty<String>

    init {
        changelogFile.convention("CHANGELOG.md")
        versionType.convention("stable")

        curseforgeId.convention(project.providers.gradleProperty("curseforge_id"))
        curseforgeApi.convention(project.providers.environmentVariable("CURSEFORGE_API"))
        modrinthId.convention(project.providers.gradleProperty("modrinth_id"))
        modrinthApi.convention(project.providers.environmentVariable("MODRINTH_API"))

        val env = project.providers.gradleProperty("environment").orElse("")
        environment.convention(this.getEnv(env.get()))

        required.convention(emptyList())
        optional.convention(emptyList())
        incompatible.convention(emptyList())
        embedded.convention(emptyList())
    }

    private fun getEnv(name: String): ModEnvironment {
        if (name.isBlank()) return ModEnvironment.CLIENT_AND_SERVER;
        return ModEnvironment.entries.firstOrNull { it.name.equals(name, ignoreCase = true) } ?: ModEnvironment.CLIENT_AND_SERVER
    }
}