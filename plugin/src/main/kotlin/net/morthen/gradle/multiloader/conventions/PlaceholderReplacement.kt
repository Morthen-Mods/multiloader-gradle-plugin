package net.morthen.gradle.multiloader.conventions

import net.morthen.gradle.multiloader.extensions.MultiloaderSettingsExtension
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.kotlin.dsl.withType
import org.gradle.language.jvm.tasks.ProcessResources

private fun isJson(path: String): Boolean {
    return listOf("json", "mcmeta").any { path.endsWith(it) }
}

internal fun Project.applyPlaceholderReplacement(settings: MultiloaderSettingsExtension) {
    tasks.withType<ProcessResources>().configureEach {
        val baseProps = mapOf<String, Any?>(
            "version" to project.version,
            "java_version" to settings.javaVersion.get(),
            "minecraft_version" to settings.minecraftVersion.get(),

            "mod_id" to settings.modId.get(),
            "mod_name" to settings.modName.get(),
            "mod_author" to settings.modAuthor.get(),
            "mod_license" to settings.modLicense.get(),
            "mod_description" to settings.modDescription.get(),
        )
        inputs.properties(baseProps)

        settings.processResourcesProperties.forEach { (patterns, extra) ->
            val resolved = extra.orEmpty().mapValues { (_, v) -> (v as? Provider<*>)?.orNull ?: v }
            inputs.properties(resolved)
            val props = baseProps + resolved

            val (jsonPatterns, otherPatterns) = patterns.partition(::isJson)

            if (jsonPatterns.isNotEmpty()) {
                filesMatching(jsonPatterns) { expand(props) { escapeBackslash.set(true) } }
            }
            if (otherPatterns.isNotEmpty()) {
                filesMatching(otherPatterns) { expand(props) }
            }
        }
    }
}