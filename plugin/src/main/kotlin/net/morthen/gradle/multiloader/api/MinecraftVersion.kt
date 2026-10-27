package net.morthen.gradle.multiloader.api

import org.gradle.api.GradleException

object MinecraftVersion {
    private const val FIRST_UNOBFUSCATED_MAJOR = 26
    private val RELEASE = Regex("""\d+\.\d+(\.\d+)?""")

    // Versions from 26.1 onwards are shipped unobfuscated.
    fun isObfuscated(version: String): Boolean = parts(version)[0] < FIRST_UNOBFUSCATED_MAJOR

    // Should mark version below 1.20.2 as legacy
    fun isLegacy(version: String): Boolean {
        val (major, minor, patch) = parts(version)
        return major == 1 && (minor < 20 || (minor == 20 && patch == 1))
    }

    fun checkSupport(version: String) {
        if (!RELEASE.matches(version)) {
            throw GradleException("'$version' is not a release version. Snapshots, pre-release and release candidates are not supported.")
        }

        val (major, minor, patch) = parts(version)
        if (major < 1 || (major == 1 && (minor < 20 || (minor == 20 && patch == 0)))) {
            throw GradleException("The Multiloader plugin only supports Minecraft 1.20.1 and higher.")
        }
    }

    // Java version required by the game: below 1.20.5 -> 17, below 26 -> 21, otherwise 25.
    fun javaVersion(version: String): Int {
        val (major, minor, patch) = parts(version)
        return when {
            major >= FIRST_UNOBFUSCATED_MAJOR -> 25
            minor > 20 || (minor == 20 && patch >= 5) -> 21
            else -> 17
        }
    }

    fun parts(version: String): List<Int> {
        val numbers = version.split('.').map { it.toIntOrNull() ?: 0 }
        return List(3) { numbers.getOrElse(it) { 0 } }
    }
}
