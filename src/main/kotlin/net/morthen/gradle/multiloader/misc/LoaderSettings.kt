package net.morthen.gradle.multiloader.misc

import net.morthen.gradle.multiloader.api.MultiloaderExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.project
import org.gradle.kotlin.dsl.the

fun applyLoaderSettings(current: Project, ext: MultiloaderExtension) = with(current) {
    val commonProject = ":common"

    // this needs to be declared early because loom resolves the compileOnly configuration
    dependencies {
        "compileOnly"(project(commonProject))
    }

    afterEvaluate {
        val javaExt = the<JavaPluginExtension>()

        // Wiring common's sources in directly as extra srcDirs (rather than only feeding them into the
        // compile task via source()/from()) is what lets the IDE's Gradle sync see them as part of this
        // sourceSet, so cross-project imports from common resolve in the editor, not just on the CLI.
        fun linkCommonSources(sourceSetName: String, javaConfigName: String, resourcesConfigName: String) {
            val javaDep = configurations.dependencyScope("${javaConfigName}Dep")
            val java = configurations.resolvable(javaConfigName) { extendsFrom(javaDep) }
            val resourcesDep = configurations.dependencyScope("${resourcesConfigName}Dep")
            val resources = configurations.resolvable(resourcesConfigName) { extendsFrom(resourcesDep) }

            dependencies {
                javaDep(project(commonProject, javaConfigName))
                resourcesDep(project(commonProject, resourcesConfigName))
            }

            val sourceSet = javaExt.sourceSets[sourceSetName]
            sourceSet.java.srcDir(java.get())
            sourceSet.resources.srcDir(resources.get())
        }

        linkCommonSources("main", "commonJava", "commonResources")

        fun configureDependencies(sourceSetName: String) {
            dependencies {
                "${sourceSetName}CompileOnly"(project(commonProject))
            }

            linkCommonSources(sourceSetName, "${sourceSetName}CommonJava", "${sourceSetName}CommonResources")
        }

        ext.testmodConfig?.let {
            configureDependencies(it.sourceSetName.get())
        }

        ext.gametestModConfig?.let {
            configureDependencies(it.sourceSetName.get())
        }
    }
}