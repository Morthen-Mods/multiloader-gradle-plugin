package net.morthen.gradle.multiloader.misc

import net.morthen.gradle.multiloader.api.MultiloaderExtension
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.project
import org.gradle.language.jvm.tasks.ProcessResources

fun applyLoaderSettings(current: Project, ext: MultiloaderExtension) = with(current) {
    val commonProject = ":common"

    // this needs to be declared early because loom resolves the compileOnly configuration
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

        fun configureDependencies(sourceSetName: String) {
            val commonJavaDep = configurations.dependencyScope("${sourceSetName}CommonJavaDep")
            val commonJava = configurations.resolvable("${sourceSetName}CommonJava") { extendsFrom(commonJavaDep) }
            val commonResourcesDep = configurations.dependencyScope("${sourceSetName}CommonResourcesDep")
            val commonResources = configurations.resolvable("${sourceSetName}CommonResources") { extendsFrom(commonResourcesDep) }

            dependencies {
                "${sourceSetName}CompileOnly"(project(commonProject)) {
                    attributes { attribute(loaderAttribute, "common") }
                }
                commonJavaDep(project(commonProject, "${sourceSetName}CommonJava"))
                commonResourcesDep(project(commonProject, "${sourceSetName}CommonResources"))
            }

            val capName = sourceSetName.replaceFirstChar(Char::uppercaseChar)

            tasks.named<JavaCompile>("compile${capName}Java").configure {
                dependsOn(commonJava)
                source(commonJava)
            }

            tasks.named<ProcessResources>("process${capName}Resources").configure {
                dependsOn(commonResources)
                from(commonResources)
            }
        }

        ext.testmodConfig?.let {
            configureDependencies(it.sourceSetName.get())
        }

        ext.gametestModConfig?.let {
            configureDependencies(it.sourceSetName.get())
        }
    }
}