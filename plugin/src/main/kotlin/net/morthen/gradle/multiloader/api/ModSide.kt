package net.morthen.gradle.multiloader.api

import me.modmuss50.mpp.platforms.modrinth.ModrinthEnvironment

/**
 * Where the mod has to be installed. Covers every Modrinth environment (ModrinthEnvironment, mod-publish-plugin 2.2.1).
 * CurseForge only knows the two tags client and server, so several values end up with the same tags there.
 */
enum class ModSide(internal val modrinth: ModrinthEnvironment, internal val curseforgeClient: Boolean, internal val curseforgeServer: Boolean) {
    // All functionality is on the client side.
    CLIENT_ONLY(ModrinthEnvironment.CLIENT_ONLY, curseforgeClient = true, curseforgeServer = false),

    // All functionality is on the server side, also present in singleplayer.
    SERVER_ONLY(ModrinthEnvironment.SERVER_ONLY, curseforgeClient = false, curseforgeServer = true),

    // Only runs on a dedicated server, no functionality in singleplayer.
    DEDICATED_SERVER_ONLY(ModrinthEnvironment.DEDICATED_SERVER_ONLY, curseforgeClient = false, curseforgeServer = true),

    // Client and server both need the mod. This is also the default value.
    CLIENT_AND_SERVER(ModrinthEnvironment.CLIENT_AND_SERVER, curseforgeClient = true, curseforgeServer = true),

    // The server needs the mod, the client can install it for extra functionality.
    SERVER_ONLY_CLIENT_OPTIONAL(ModrinthEnvironment.SERVER_ONLY_CLIENT_OPTIONAL, curseforgeClient = true, curseforgeServer = true),

    // The client needs the mod, the server can install it for extra functionality.
    CLIENT_ONLY_SERVER_OPTIONAL(ModrinthEnvironment.CLIENT_ONLY_SERVER_OPTIONAL, curseforgeClient = true, curseforgeServer = true),

    // Runs on client or server, extra functionality if both have it.
    CLIENT_OR_SERVER_PREFERS_BOTH(ModrinthEnvironment.CLIENT_OR_SERVER_PREFERS_BOTH, curseforgeClient = true, curseforgeServer = true),

    // Runs on client or server, no extra functionality if both have it.
    CLIENT_OR_SERVER(ModrinthEnvironment.CLIENT_OR_SERVER, curseforgeClient = true, curseforgeServer = true),

    // Only runs in singleplayer, no functionality in multiplayer.
    SINGLEPLAYER_ONLY(ModrinthEnvironment.SINGLEPLAYER_ONLY, curseforgeClient = true, curseforgeServer = false),
}