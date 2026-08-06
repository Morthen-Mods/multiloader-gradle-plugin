package net.morthen.gradle.multiloader.plugins

import net.minecraftforge.gradle.ForgeGradleExtension
import net.minecraftforge.gradle.MinecraftExtension
import net.minecraftforge.gradle.MinecraftExtensionForProject
import net.minecraftforge.gradle.SlimeLauncherOptions
import net.morthen.gradle.multiloader.api.MultiloaderExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.repositories
import org.gradle.kotlin.dsl.the

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
 */
private fun SlimeLauncherOptions.configureBootstrapLaunch(client: Boolean) {
    mainClass.set("net.minecraftforge.bootstrap.ForgeBootstrap")
    jvmArgs.set(listOf("-Djava.net.preferIPv6Addresses=system", "-XX:+UseCompactObjectHeaders"))
    systemProperty("forge.enableGameTest", "true")
    environment("MCP_MAPPINGS", "{mcp_mappings}")

    args("--gameDir", ".", "--launchTarget", if (client) "forge_userdev_client" else "forge_userdev_server")
    if (client) {
        args("--version", "MOD_DEV", "--assetIndex", "{asset_index}", "--assetsDir", "{assets_root}")
    }
}

fun applyForgeGradle(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(ForgeGradle.PLUGIN_ID)

    val java = the<JavaPluginExtension>()

    // The dumb hacky fix, which should be already fixed as stated by Lex
    // But surprise it isn't
    java.sourceSets.configureEach {
        val dir = layout.buildDirectory.dir("sourcesSets/$name")
        output.setResourcesDir(dir.get().asFile)
        getJava().destinationDirectory.set(dir)
    }

    val repMC = the<MinecraftExtension>()
    val minecraft = the<MinecraftExtensionForProject>()
    val fg = the<ForgeGradleExtension>()

    repositories {
        repMC.mavenizer(this)
        maven(fg.forgeMaven)
        maven(fg.minecraftLibsMaven)
        mavenCentral()
    }

    dependencies {
        "implementation"(minecraft.dependency("net.minecraftforge:forge:${ext.minecraftVersion.get()}-${ext.forgeVersion.get()}"))
    }

    minecraft.runs {
        register("client") {
            workingDir.set(ext.runDir("client"))
            ext.forgeMixins.get().forEach { mixin -> args("--mixin.config=$mixin") }
        }

        register("server") {
            workingDir.set(ext.runDir("server"))
            ext.forgeMixins.get().forEach { mixin -> args("--mixin.config=$mixin") }
        }

        ext.testmodConfig?.let { testmod ->
            if (testmod.clientRun.get()) {
                register("testmodClient") {
                    workingDir.set(ext.runDir("client"))
                    configureBootstrapLaunch(client = true)
                    ext.forgeMixins.get().forEach { mixin -> args("--mixin.config=$mixin") }
                }
            }

            if (testmod.serverRun.get()) {
                register("testmodServer") {
                    workingDir.set(ext.runDir("server"))
                    configureBootstrapLaunch(client = false)
                    ext.forgeMixins.get().forEach { mixin -> args("--mixin.config=$mixin") }
                }
            }
        }
    }

    // ForgeGradle's run tasks only put getDefaultSourceSets() (main-only, or main+test for the
    // auto-generated per-sourceSet task variants) on the launch classpath; mods{} above does not
    // affect it. Adding testmod's compiled output directly to the auto-generated task's own (public,
    // standard Gradle) classpath is what actually gets it in front of FML's mod scanner.
    ext.testmodConfig?.let { testmod ->
        afterEvaluate {
            val testmodOutput = java.sourceSets[testmod.sourceSetName.get()].output

            if (testmod.clientRun.get()) {
                tasks.named<JavaExec>("runTestmodClient") { classpath(testmodOutput) }
            }

            if (testmod.serverRun.get()) {
                tasks.named<JavaExec>("runTestmodServer") { classpath(testmodOutput) }
            }
        }
    }
}