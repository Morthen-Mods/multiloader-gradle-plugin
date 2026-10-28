package net.morthen.gradle.multiloader.conventions

import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.the
import org.gradle.language.jvm.tasks.ProcessResources

fun applyCompileSettings(target: Project, ext: MultiloaderExtension) = with(target) {
    val commonProject = ":common" //TODO: make that changeable with a Property

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
            sourceSet.java.srcDir(target.project(commonProject).file("src/$sourceSetName/java"))
            sourceSet.resources.srcDir(target.project(commonProject).file("src/$sourceSetName/resources"))

            dependencies {
                "${sourceSetName}CompileOnly"(project(commonProject)) {
                    attributes { attribute(loaderAttribute, "common") }
                }
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