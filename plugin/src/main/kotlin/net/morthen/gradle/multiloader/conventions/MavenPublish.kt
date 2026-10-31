package net.morthen.gradle.multiloader.conventions

import net.morthen.gradle.multiloader.extensions.MultiloaderSettingsExtension
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.the

fun Project.applyMavenPublish(settings: MultiloaderSettingsExtension, loader: String) {
    the<PublishingExtension>().apply {
        publications {
            register<MavenPublication>("mavenJava") {
                artifactId = "${ settings.modId.get() }-${ loader }"
                from(components["java"])
            }
        }

        providers.environmentVariable("MAVEN_URL").orNull?.let { url ->
            repositories {
                maven(url) {
                    credentials {
                        username = providers.environmentVariable("MAVEN_USER").orNull
                        password = providers.environmentVariable("MAVEN_PASSWORD").orNull
                    }
                }
            }
        }
    }
}