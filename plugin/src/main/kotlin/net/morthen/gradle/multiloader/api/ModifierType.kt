package net.morthen.gradle.multiloader.api

enum class ModifierType(private val extension: String) {
    CLASS_TWEAKER("classtweaker"),
    ACCESS_WIDENER("accesswidener");

    fun getFileName(modId: String): String {
        return "$modId.${extension}"
    }
}