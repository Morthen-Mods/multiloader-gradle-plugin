package net.morthen.gradle.multiloader.plugins

import net.minecraftforge.gradle.ForgeGradleExtension
import net.minecraftforge.gradle.MinecraftExtension
import net.minecraftforge.gradle.MinecraftExtensionForProject
import net.minecraftforge.gradle.SlimeLauncherOptions
import net.morthen.gradle.multiloader.api.MultiloaderExtension
import net.neoforged.moddevgradle.dsl.DataFileCollection
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.named
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
 */
private fun SlimeLauncherOptions.configureBootstrapLaunch(client: Boolean) {
    mainClass.set("net.minecraftforge.bootstrap.ForgeBootstrap")
    jvmArgs.set(listOf("-Djava.net.preferIPv6Addresses=system", "-XX:+UseCompactObjectHeaders"))
    environment("MCP_MAPPINGS", "{mcp_mappings}")

    args("--gameDir", ".", "--launchTarget", if (client) "forge_userdev_client" else "forge_userdev_server")
    if (client) {
        args("--version", "MOD_DEV", "--assetIndex", "{asset_index}", "--assetsDir", "{assets_root}")
    }
}

fun addIfExists(collection: ConfigurableFileCollection, target: Project, relativePath: String) {
    val commonFile = target.project(":common").file(relativePath)
    val selfFile = target.file(relativePath)

    if (commonFile.exists()) collection.from(commonFile)
    if (selfFile.exists()) collection.from(selfFile)
}

fun applyForgeGradle(target: Project, ext: MultiloaderExtension) = with(target) {
    pluginManager.apply(ForgeGradle.PLUGIN_ID)

    val java = the<JavaPluginExtension>()

    // The dumb hacky fix, which should be already fixed as stated by Lex
    // But surprise it isn't
    java.sourceSets.configureEach {
        val dir = layout.buildDirectory.dir("sourcesSets/$name")
        output.setResourcesDir(dir.get())
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

    addIfExists(minecraft.accessTransformers, target, "src/main/resources/META-INF/accesstransformer.cfg")

    minecraft.runs {
        configureEach {
            if (name != "data") {
                val upperName = name.replaceFirstChar { it.uppercase() }
                rootProject.the(IdeaModel::class).project.settings.runConfigurations.create<Gradle>("Forge $upperName") {
                    taskNames = listOf(":${project.name}:run$upperName")
                    setProject(project)
                }
            }
        }

        register("client") {
            workingDir.set(ext.runDir("client"))
            ext.forgeMixins.get().forEach { mixin -> args("--mixin.config=$mixin") }
        }

        register("server") {
            workingDir.set(ext.runDir("server"))
            ext.forgeMixins.get().forEach { mixin -> args("--mixin.config=$mixin") }
            args("--nogui")
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
                    args("--nogui")
                }
            }
        }

        ext.gametestModConfig?.let { gametest ->
            register("gameTestServer") {
                workingDir.set(ext.runDir("server"))
                systemProperty("forge.enableGameTest", "True")
                systemProperty("forge.enableGameTestNamespaces", gametest.modId.get());
                ext.forgeMixins.get().forEach { mixin -> args("--mixin.config=$mixin") }
            }

            // Gradle run will not be registered, since it is only a requirement for the GameTestServer run.
            // Forge doesn't apply the json data needed via Code, so we have to generate it before running the Gametest Server.
            register("data") {
                workingDir.set(ext.runDir("data"))
                args("--mod", gametest.modId.get(), "--all", "--output", file("src/${ gametest.sourceSetName.get() }/generated").absolutePath)
            }
        }
    }

    afterEvaluate {
        // ForgeGradle's run tasks only put getDefaultSourceSets() (main-only, or main+test for the
        // auto-generated per-sourceSet task variants) on the launch classpath; mods{} above does not
        // affect it. Adding testmod's compiled output directly to the auto-generated task's own (public,
        // standard Gradle) classpath is what actually gets it in front of FML's mod scanner.
        ext.testmodConfig?.let { testmod ->
            val tmOutput = java.sourceSets[testmod.sourceSetName.get()].output

            if (testmod.clientRun.get()) {
                tasks.named<JavaExec>("runTestmodClient") { classpath(tmOutput) }
            }

            if (testmod.serverRun.get()) {
                tasks.named<JavaExec>("runTestmodServer") { classpath(tmOutput) }
            }
        }

        ext.gametestModConfig?.let { gametest ->
            val gOutput = java.sourceSets[gametest.sourceSetName.get()].output

            tasks.named<JavaExec>("runData") { classpath(gOutput) }
            tasks.named<JavaExec>("runGameTestServer") {
                classpath(gOutput)
                dependsOn(":${ project.name }:runData")
            }

            java.sourceSets[gametest.sourceSetName.get()].resources { srcDir("src/${ gametest.sourceSetName.get() }/generated/") }
        }

        tasks.named<Jar>("jar") {
            manifest {
                attributes(mapOf<String, Any>(
                    "MixinConfig" to ext.forgeMixins.map { it.joinToString(",") }
                ))
            }
        }
    }
}