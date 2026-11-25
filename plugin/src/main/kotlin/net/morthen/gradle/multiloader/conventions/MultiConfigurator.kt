package net.morthen.gradle.multiloader.conventions

import org.gradle.api.Project

internal enum class MultiConfiguration(val target: String) {
    API("api"),
    IMPLEMENTATION("implementation"),
    COMPILE_ONLY("compileOnly"),
    COMPILE_ONLY_API("compileOnlyApi"),
    RUNTIME_ONLY("runtimeOnly");

    private val capitalized = target.replaceFirstChar { it.uppercase() }
    val bucket = "multi$capitalized"
    val modTarget = "mod$capitalized"
}

internal fun Project.registerMultiConfigurations() {
    MultiConfiguration.entries.forEach { configurations.dependencyScope(it.bucket) }
}

internal fun Project.connectMultiConfiguration(remap: Boolean, copy: Boolean = false) {
    MultiConfiguration.entries.forEach { type ->
        val bucket = configurations.getByName(type.bucket)
        configurations.named(if (remap) type.modTarget else type.target) {
            if (copy) {
                dependencies.addAllLater(provider { bucket.allDependencies.toList() })
                dependencyConstraints.addAllLater(provider { bucket.allDependencyConstraints.toList() })
            } else {
                extendsFrom(bucket)
            }
        }
    }
}