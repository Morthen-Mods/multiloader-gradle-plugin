package net.morthen.gradle.multiloader.conventions

import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources

fun applyCompileSettings(target: Project, ext: MultiloaderExtension) = with(target) {
    val commonProject = ":${ ext.settings.commonProject.get() }"

    afterEvaluate {
        // Links a source set of this loader with the source set of the same name in common
        fun configureDependencies(sourceSet: SourceSet) {
            val isMain = sourceSet.name == SourceSet.MAIN_SOURCE_SET_NAME
            val prefix = if (isMain) "common" else "common" + sourceSet.name.replaceFirstChar { it.uppercase() }

            // IDE + compile classpath
            dependencies {
                sourceSet.compileOnlyConfigurationName(project(commonProject)) {
                    attributes { attribute(loaderAttribute, "common") }
                }
                // main is the default capability, only custom sources need their feature variant
                if (!isMain) sourceSet.compileOnlyConfigurationName(project(commonProject)) {
                    capabilities { requireFeature(sourceSet.name) }
                    attributes { attribute(loaderAttribute, "common") }
                }
            }

            // Build: sources and resources of common only on task level, so the IDE doesn't see shared roots
            val javaDep = configurations.dependencyScope("${prefix}JavaDep")
            val javaSrc = configurations.resolvable("${prefix}Java") { extendsFrom(javaDep) }
            val resDep = configurations.dependencyScope("${prefix}ResourcesDep")
            val resSrc = configurations.resolvable("${prefix}Resources") { extendsFrom(resDep) }

            dependencies {
                javaDep(project(commonProject, "${prefix}Java"))
                resDep(project(commonProject, "${prefix}Resources"))
            }

            tasks.named<JavaCompile>(sourceSet.compileJavaTaskName) {
                dependsOn(javaSrc)
                source(javaSrc)
            }
            tasks.named<ProcessResources>(sourceSet.processResourcesTaskName) {
                dependsOn(resSrc)
                from(resSrc)
            }

            // javadoc and sourcesJar only exist for main
            if (isMain) {
                tasks.named<Javadoc>(sourceSet.javadocTaskName) {
                    dependsOn(javaSrc)
                    source(javaSrc)
                }
                tasks.named<Jar>(sourceSet.sourcesJarTaskName) {
                    dependsOn(javaSrc, resSrc)
                    from(javaSrc, resSrc)
                }
            }
        }

        val sourceSets = the<JavaPluginExtension>().sourceSets
        listOfNotNull(
            SourceSet.MAIN_SOURCE_SET_NAME,
            ext.settings.testmod?.sourceSet?.get(),
            ext.settings.gametest?.sourceSet?.get()
        ).forEach { configureDependencies(sourceSets[it]) }
    }
}