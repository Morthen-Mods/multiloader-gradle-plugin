package net.morthen.gradle.multiloader.api

import net.morthen.gradle.multiloader.dsl.Run

interface RunApplier {
    fun apply(run: Run)
}