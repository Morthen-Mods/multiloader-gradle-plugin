package net.morthen.gradle.multiloader.api

object MinecraftVersion {
    private const val FIRST_UNOBFUSCATED_MAJOR = 26

    /** Versions from 26.1 onwards are shipped unobfuscated. */
    fun isObfuscated(version: String): Boolean = parts(version)[0] < FIRST_UNOBFUSCATED_MAJOR

    /** Java version required by the game: below 1.20.5 -> 17, below 26 -> 21, otherwise 25. */
    fun javaVersion(version: String): Int {
        val (major, minor, patch) = parts(version)
        return when {
            major >= FIRST_UNOBFUSCATED_MAJOR -> 25
            minor > 20 || (minor == 20 && patch >= 5) -> 21
            else -> 17
        }
    }

    private fun parts(version: String): List<Int> {
        val numbers = version.split('.').map { it.toIntOrNull() ?: 0 }
        return List(3) { numbers.getOrElse(it) { 0 } }
    }
}
