package net.morthen.gradle.multiloader.api

import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class TestmodConfiguration @Inject constructor(ext: MultiloaderExtension){
    abstract val modId: Property<String>
    abstract val sourceSetName: Property<String>

    abstract val serverRun: Property<Boolean>
    abstract val clientRun: Property<Boolean>

    init {
        modId.convention(ext.modId.map { "${ it }_testmod" })
        sourceSetName.convention("testmod")

        serverRun.convention(true)
        clientRun.convention(true)
    }
}