@file:Suppress("UnstableApiUsage")

package net.morthen.gradle.multiloader

import net.morthen.gradle.multiloader.api.MultiloaderExtension
import net.morthen.gradle.multiloader.misc.applyDefaultRepositories
import net.morthen.gradle.multiloader.misc.applyGradleConvention
import net.morthen.gradle.multiloader.misc.applyLoaderSettings
import net.morthen.gradle.multiloader.misc.applyModPublishSettings
import net.morthen.gradle.multiloader.plugins.*
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.component.AdhocComponentWithVariants
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.provider.Provider
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

@Suppress("unused")
abstract class MultiloaderPlugin : Plugin<Project> {

    override fun apply(target: Project) = with(target) {
        pluginManager.apply("java-library")
        pluginManager.apply("maven-publish")
        pluginManager.apply("version-catalog")

        rootProject.pluginManager.apply("idea")
        rootProject.pluginManager.apply("org.jetbrains.gradle.plugin.idea-ext")

        applyDefaultRepositories()

        val ext = extensions.create<MultiloaderExtension>("multiloader")
        val javaExt = the<JavaPluginExtension>()
        val base = the<BasePluginExtension>()

        javaExt.toolchain.languageVersion.set(JavaLanguageVersion.of(ext.javaVersion.get()))
        javaExt.withSourcesJar()

        project.version = "${ ext.minecraftVersion.get() }-${ project.version }"
        base.archivesName.set("${ ext.modId.get() }-${ target.name }")

        tasks.withType<JavaCompile>().configureEach {
            options.release = ext.javaVersion

            // docs.oracle.com/en/java/javase/25/docs/specs/man/javac.html#options
            val xlint = listOf(
                "cast", // unnecessary casts
                "dangling-doc-comments", // dangling javadoc
                "text-blocks", // inconsistent whitespace in textblocks
                "dep-ann", // deprecated in javadoc but no @Deprecated annotation
                "empty", // empty if statements
                "overrides",
                "deprecation",
                "removal",
                "rawtypes",
                "unchecked",
                "static", // static method access using object instance
                "varargs",
            )
            options.compilerArgs.addAll(listOf(
                "-Xmaxerrs", "500",
                "-Xmaxwarns", "500",
                "-Xlint:${xlint.joinToString(",")}",
                "-Xpkginfo:nonempty", // only emit package-info.class if it contains class or runtime scope annotations
            ))
        }

        tasks.withType<Javadoc>().configureEach {
            (options as StandardJavadocDocletOptions).tags(listOf("reason", "implNote"))
        }

        tasks.withType<ProcessResources>().configureEach {
            filteringCharset = "UTF-8"
        }

        // the `multiloader { }` block in the consumer's build script runs after apply(),
        // so ext.loader can only be read once the project has finished evaluating.
        afterEvaluate {
            val loader = ext.loader.get()
            applyGradleConvention(loader)

            when (loader) {
                "common" -> {
                    applyCommonModDevGradle(this@with, ext)
                }
                "datagen" -> {
                    applyGradleConvention("neoforge")
                    applyDatagenModDevGradle(this@with, ext)
                    applyLoaderSettings(this@with, ext)
                }
                "fabric" -> {
                    applyFabricLoom(this@with, ext)
                    applyLoaderSettings(this@with, ext)
                    applyModPublishSettings(this@with, ext)
                }
                "forge" -> {
                    applyForgeGradle(this@with, ext)
                    applyLoaderSettings(this@with, ext)
                    applyModPublishSettings(this@with, ext)
                }
                "neoforge" -> {
                    applyModDevGradle(this@with, ext)
                    applyLoaderSettings(this@with, ext)
                    applyModPublishSettings(this@with, ext)
                }
                else -> throw GradleException("Unsupported multiloader.loader '$loader', expected one of: common, datagen, fabric, forge, neoforge")
            }

            val javaComponent = components["java"] as AdhocComponentWithVariants
            listOfNotNull(ext.testmodConfig?.sourceSetName?.get(), ext.gametestModConfig?.sourceSetName?.get()).forEach { sourceSetName ->
                javaComponent.withVariantsFromConfiguration(configurations["${sourceSetName}ApiElements"]) { skip() }
                javaComponent.withVariantsFromConfiguration(configurations["${sourceSetName}RuntimeElements"]) { skip() }
            }

            the<PublishingExtension>().apply {
                publications {
                    register<MavenPublication>("mavenJava") {
                        artifactId = "${ ext.modId.get() }-${ ext.loader.get() }"
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

            fun isJson(path: String): Boolean {
                return listOf("json", "mcmeta").any { path.endsWith(it) }
            }

            target.tasks.withType(ProcessResources::class.java).configureEach {
                val expandProps = mapOf<String, Any?>(
                    "version" to project.version,
                    "java_version" to ext.javaVersion.get(),
                    "minecraft_version" to ext.minecraftVersion.get(),

                    "mod_id" to ext.modId.get(),
                    "mod_name" to ext.modName.get(),
                    "mod_author" to ext.modAuthor.get(),
                    "mod_license" to ext.modLicense.get(),
                    "mod_description" to ext.modDescription.get(),
                )

                inputs.properties(expandProps)

                ext.processResourcesProperties.forEach { (patterns, extraProperties) ->
                    val finalProps = mutableMapOf<String, Any?>()
                    finalProps.putAll(expandProps)
                    extraProperties?.mapValues { entry -> if(entry.value is Provider<*>) (entry.value as Provider<*>).orNull else entry.value }?.let { map ->
                        inputs.properties(map)
                        finalProps.putAll(map)
                    }

                    patterns.filter(::isJson).let { filters ->
                        if(filters.isNotEmpty()) {
                            filesMatching(filters) {
                                expand(finalProps) {
                                    escapeBackslash = true
                                }
                            }
                        }
                    }
                    patterns.filterNot(::isJson).let { filters ->
                        if(filters.isNotEmpty()) {
                            filesMatching(filters) {
                                expand(finalProps)
                            }
                        }
                    }
                }
            }
        }
    }
}
