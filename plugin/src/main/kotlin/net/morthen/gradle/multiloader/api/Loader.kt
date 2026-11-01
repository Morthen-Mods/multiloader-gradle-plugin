package net.morthen.gradle.multiloader.api

import java.util.Locale.getDefault

enum class Loader {
    COMMON, DATAGEN, FABRIC, FORGE, NEOFORGE;

    fun getId() = name.lowercase(getDefault())
    fun getName() = getId().replaceFirstChar { it.uppercase() }
}