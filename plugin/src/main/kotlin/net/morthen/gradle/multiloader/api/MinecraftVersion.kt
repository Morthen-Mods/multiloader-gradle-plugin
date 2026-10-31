package net.morthen.gradle.multiloader.api

import org.gradle.api.GradleException

object MinecraftVersion {
    private const val FIRST_UNOBFUSCATED = "26"
    private val RELEASE = Regex("""\d+\.\d+(\.\d+)?""")

    // Versions from 26.1 onwards are shipped unobfuscated.
    fun isObfuscated(version: String): Boolean = isBefore(version, FIRST_UNOBFUSCATED)

    // Versions below 1.20.2 are legacy.
    fun isLegacy(version: String): Boolean = isBefore(version, "1.20.2")

    // True if mcVersion is strictly older than checkVersion ("1.21.3" before "1.21.4" -> true, "1.21.4" before "1.21.4" -> false).
    fun isBefore(mcVersion: String, checkVersion: String): Boolean = compare(mcVersion, checkVersion) < 0

    fun checkSupport(version: String) {
        if (!RELEASE.matches(version)) {
            throw GradleException("'$version' is not a release version. Snapshots, pre-release and release candidates are not supported.")
        }

        if (isBefore(version, "1.20.1")) {
            throw GradleException("The Multiloader plugin only supports Minecraft 1.20.1 and higher.")
        }
    }

    // Java version required by the game: below 1.20.5 -> 17, below 26 -> 21, otherwise 25.
    // TODO: Expand to eventually support every mcVersion
    fun javaVersion(version: String): Int = when {
        !isBefore(version, FIRST_UNOBFUSCATED) -> 25
        !isBefore(version, "1.20.5") -> 21
        else -> 17
    }

    // Split the given mcVersion into 3 parts
    fun parts(version: String): List<Int> {
        val numbers = version.split('.').map { it.toIntOrNull() ?: 0 }
        return List(3) { numbers.getOrElse(it) { 0 } }
    }

    // < 0: a is older, 0: equal, > 0: a is newer. Missing parts count as 0 ("1.21" == "1.21.0", "26" == "26.0.0").
    private fun compare(a: String, b: String): Int =
        parts(a).zip(parts(b))
            .map { (x, y) -> x.compareTo(y) }
            .firstOrNull { it != 0 } ?: 0
}
