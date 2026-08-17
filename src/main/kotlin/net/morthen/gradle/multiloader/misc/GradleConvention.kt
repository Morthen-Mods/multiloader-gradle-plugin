package net.morthen.gradle.multiloader.misc

import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.the
import org.gradle.kotlin.dsl.withType

val loaderAttribute = Attribute.of("io.github.mcgradleconventions.loader", String::class.java)

fun Project.applyGradleConvention(loader: String) {
    buildList {
        add("apiElements")
        add("runtimeElements")
        add("sourcesElements")
    }.forEach { configName ->
        configurations.named(configName).configure {
            attributes { attribute(loaderAttribute, loader) }
        }

        the(PublishingExtension::class).publications.withType<MavenPublication>().configureEach {
            suppressPomMetadataWarningsFor(configName)
        }
    }

    the(JavaPluginExtension::class).sourceSets.configureEach {
        listOf(compileClasspathConfigurationName, runtimeClasspathConfigurationName).forEach {
            configurations.named(it) {
                attributes { attribute(loaderAttribute, loader) }
            }
        }
    }
}