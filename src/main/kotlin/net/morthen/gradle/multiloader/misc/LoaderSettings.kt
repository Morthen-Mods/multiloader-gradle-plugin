package net.morthen.gradle.multiloader.misc

import net.morthen.gradle.multiloader.api.MultiloaderExtension
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

        // Feature source sets (testmod/gametest) mirror common's same-named source set by adding
        // its directories directly, rather than merging via task.source()/from() like main does above.
        // Gradle task.source()/from() additions only take effect when the task actually runs, so
        // IntelliJ's Gradle sync (which never runs tasks) can't see them and reports the common
        // classes as unresolved while editing. A real sourceSet.srcDir() is part of the declared
        // project model, so both javac and the IDE see the same source root.
        fun configureDependencies(sourceSetName: String) {
            val sourceSet = the<JavaPluginExtension>().sourceSets[sourceSetName]
            sourceSet.java.srcDir(current.project(commonProject).file("src/$sourceSetName/java"))
            sourceSet.resources.srcDir(current.project(commonProject).file("src/$sourceSetName/resources"))

            dependencies {
                "${sourceSetName}CompileOnly"(project(commonProject)) {
                    attributes { attribute(loaderAttribute, "common") }
                }
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