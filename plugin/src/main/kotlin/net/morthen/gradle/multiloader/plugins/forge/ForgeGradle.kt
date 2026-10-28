package net.morthen.gradle.multiloader.plugins.forge

import net.fabricmc.loom.task.LoomTasks
import net.minecraftforge.gradle.ForgeGradleExtension
import net.minecraftforge.gradle.MinecraftExtension
import net.minecraftforge.gradle.MinecraftExtensionForProject
import net.minecraftforge.gradle.SlimeLauncherOptions
import net.morthen.gradle.multiloader.extensions.MultiloaderExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.the
import org.gradle.plugins.ide.idea.model.IdeaModel
import org.jetbrains.gradle.ext.Gradle
import org.jetbrains.gradle.ext.runConfigurations
import org.jetbrains.gradle.ext.settings

object ForgeGradle {
    const val PLUGIN_ID = "net.minecraftforge.gradle"
}

/**
 * ForgeGradle 7 only resolves `mainClass`/launch args for runs whose registered name matches a key
 * in the `runs.json` bundled with the resolved Forge userdev artifact (`client`/`server`/`data`/...).
 * Custom-named runs get no such entry and fail at execution time with a missing `mainClass`, so for
 * `testmodClient`/`testmodServer` we replicate what FG itself resolves for `client`/`server`.
 * Captured by running `runClient`/`runServer` with `--info` against forge 26.1.2-64.1.0
 * (forgegradle 7.0.30) - this is inherently version-coupled and may need re-capturing on Forge/MC bumps.
 *
 * <Implementation assisted by Claude-Sonnet-5.5>
 */
fun SlimeLauncherOptions.configureBootstrapLaunch(client: Boolean) {
    mainClass.set("net.minecraftforge.bootstrap.ForgeBootstrap")
    jvmArgs.set(listOf("-Djava.net.preferIPv6Addresses=system", "-XX:+UseCompactObjectHeaders"))
    environment("MCP_MAPPINGS", "{mcp_mappings}")

    args("--gameDir", ".", "--launchTarget", if (client) "forge_userdev_client" else "forge_userdev_server")
    if (client) {
        args("--version", "MOD_DEV", "--assetIndex", "{asset_index}", "--assetsDir", "{assets_root}")
    }
}

fun applyForgeGradle(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(ForgeGradle.PLUGIN_ID)
    val repMC = the<MinecraftExtension>()
    val minecraft = the<MinecraftExtensionForProject>()
    val fg = the<ForgeGradleExtension>()

    val mc = ext.settings.minecraftVersion.get()
    val forge = ext.settings.forgeVersion.get()

    repositories {
        repMC.mavenizer(this)
        maven(fg.forgeMaven)
        maven(fg.minecraftLibsMaven)
        mavenCentral()
    }

    dependencies {
        "implementation"(minecraft.dependency("net.minecraftforge:forge:${ mc }-${ forge }"))
    }

    ext.settings.runs.forEach { run ->
        ForgeGradleRunApplier(minecraft).apply(run)

        val upperName = run.name.replaceFirstChar { it.uppercase() }
        rootProject.the(IdeaModel::class).project.settings.runConfigurations.create<Gradle>(run.displayName.get()) {
            taskNames = listOf(":${target.name}:run$upperName")
            setProject(target)
        }
    }

    // The dumb hacky fix, which should be already fixed as stated by Lex
    // But surprise it isn't
    val java = the<JavaPluginExtension>()
    java.sourceSets.configureEach {
        val dir = layout.buildDirectory.dir("sourcesSets/$name")
        output.setResourcesDir(dir.get())
        getJava().destinationDirectory.set(dir)
    }
}