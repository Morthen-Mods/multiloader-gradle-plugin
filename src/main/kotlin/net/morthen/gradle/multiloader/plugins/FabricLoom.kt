package net.morthen.gradle.multiloader.plugins

import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.api.fabricapi.FabricApiExtension
import net.fabricmc.loom.task.LoomTasks
import net.morthen.gradle.multiloader.api.MultiloaderExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.assign
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.invoke
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

object FabricLoom {
    const val PLUGIN_ID = "net.fabricmc.fabric-loom"
}

fun applyFabricLoom(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(FabricLoom.PLUGIN_ID)

    rootProject.pluginManager.apply("idea")
    rootProject.pluginManager.apply("org.jetbrains.gradle.plugin.idea-ext")

    val loom = the<LoomGradleExtensionAPI>()
    val fapi = the<FabricApiExtension>()

    dependencies {
        "minecraft"("com.mojang:minecraft:${ ext.minecraftVersion.get() }")
        "implementation"("net.fabricmc:fabric-loader:${ ext.fabricLoaderVersion.get() }")
        "implementation"("net.fabricmc.fabric-api:fabric-api:${ ext.fabricApiVersion.get() }")
    }

    loom.runConfigs {
        named("client") {
            client()
            displayName = "Fabric Client"
            runDirectory = ext.runDir("client")
        }

        named("server") {
            server()
            displayName = "Fabric Server"
            runDirectory = ext.runDir("server")
        }
    }

    loom.runConfigs.configureEach {
        systemProperties.put("fabric-tag-conventions-v2.missingTagTranslationWarning", "VERBOSE")

        generateRunConfig = false
        rootProject.the(IdeaModel::class).project.settings.runConfigurations.create<Gradle>(displayName.get()) {
            taskNames = listOf(LoomTasks.getRunConfigTaskName(this@configureEach))
            setProject(project)
        }
    }

    loom.mods {
        create(ext.modId.get()) {
            sourceSet("main")
        }
    }

//    fapi.configureTests {
//        createSourceSet = false
//        modId = "${ ext.modId.get() }_gametest"
//        eula = true
//    }
}