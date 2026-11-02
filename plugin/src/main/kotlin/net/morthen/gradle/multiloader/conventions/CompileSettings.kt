package net.morthen.gradle.multiloader.conventions

import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

fun applyCompileSettings(target: Project, ext: MultiloaderExtension) = with(target) {
    val commonProject = ":${ ext.settings.commonProject.get() }"

    dependencies {
        "compileOnly"(project(commonProject)) {
            attributes { attribute(loaderAttribute, "common") }
        }
    }

    afterEvaluate {
        val commonJavaDep = configurations.dependencyScope("commonJavaDep")
        val commonJava = configurations.resolvable("commonJava") { extendsFrom(commonJavaDep) }

        val commonResourcesDep = configurations.dependencyScope("commonResourcesDep")
        val commonResources = configurations.resolvable("commonResources") { extendsFrom(commonResourcesDep) }

        dependencies {
            commonJavaDep(project(commonProject, "commonJava"))
            commonResourcesDep(project(commonProject, "commonResources"))
        }

        tasks.named<JavaCompile>("compileJava").configure {
            dependsOn(commonJava)
            source(commonJava)
        }

        tasks.named<ProcessResources>("processResources").configure {
            dependsOn(commonResources)
            from(commonResources)
        }

        tasks.named<Javadoc>("javadoc").configure {
            dependsOn(commonJava)
            source(commonJava)
        }

        tasks.named<Jar>("sourcesJar").configure {
            dependsOn(commonJava, commonResources)
            from(commonJava, commonResources)
        }

        // make common
        fun configureDependencies(sourceSetName: String) {
            val sourceSet = the<JavaPluginExtension>().sourceSets[sourceSetName]
            val cap = sourceSetName.replaceFirstChar { it.uppercase() }

            dependencies {
                sourceSet.compileOnlyConfigurationName(project(commonProject)) {
                    attributes { attribute(loaderAttribute, "common") }
                }
                sourceSet.compileOnlyConfigurationName(project(commonProject)) {
                    capabilities { requireFeature(sourceSetName) }
                    attributes { attribute(loaderAttribute, "common") }
                }
            }

            val javaDep = configurations.dependencyScope("common${cap}JavaDep")
            val javaSrc = configurations.resolvable("common${cap}Java") { extendsFrom(javaDep) }
            val resDep = configurations.dependencyScope("common${cap}ResourcesDep")
            val resSrc = configurations.resolvable("common${cap}Resources") { extendsFrom(resDep) }

            dependencies {
                javaDep(project(commonProject, "common${cap}Java"))
                resDep(project(commonProject, "common${cap}Resources"))
            }

            tasks.named<JavaCompile>(sourceSet.compileJavaTaskName) {
                dependsOn(javaSrc)
                source(javaSrc)
            }
            tasks.named<ProcessResources>(sourceSet.processResourcesTaskName) {
                dependsOn(resSrc)
                from(resSrc)
            }
        }

        ext.settings.testmod?.let {
            configureDependencies(it.sourceSet.get())
        }

        ext.settings.gametest?.let {
            configureDependencies(it.sourceSet.get())
        }
    }
}