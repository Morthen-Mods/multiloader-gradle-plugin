package net.morthen.gradle.multiloader.misc

import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.project
import org.gradle.language.jvm.tasks.ProcessResources

fun applyLoaderSettings(current: Project) = with(current) {
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
    }
}