package net.morthen.gradle.multiloader.api

import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class ModPublishConfiguration @Inject constructor(project: Project) {
    abstract val changelogFile: Property<String>
    abstract val versionType: Property<String>

    abstract val curseforgeId: Property<String>
    abstract val curseforgeApi: Property<String>
    abstract val modrinthId: Property<String>
    abstract val modrinthApi: Property<String>

    abstract val client: Property<Boolean>
    abstract val server: Property<Boolean>

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

        client.convention(true)
        server.convention(true)

        required.convention(emptyList())
        optional.convention(emptyList())
        incompatible.convention(emptyList())
        embedded.convention(emptyList())
    }
}