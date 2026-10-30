package net.morthen.gradle.multiloader.configurator.forge

import net.minecraftforge.gradle.SlimeLauncherOptions

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