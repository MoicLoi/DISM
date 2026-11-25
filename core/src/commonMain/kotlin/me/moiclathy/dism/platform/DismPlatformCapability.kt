package me.moiclathy.dism.platform

import me.moiclathy.dism.api.DismPublicApi

/**
 * Capabilities provided by the host platform.
 */
@DismPublicApi
enum class DismPlatformCapability {

    /**
     * Persistent local storage is available.
     */
    LOCAL_STORAGE,

    /**
     * Background execution is available.
     */
    BACKGROUND_EXECUTION,

    /**
     * Network access can be provided.
     */
    NETWORK,

    /**
     * File-system access is available.
     */
    FILE_SYSTEM,

    /**
     * Platform notification facilities are available.
     */
    NOTIFICATIONS,

    /**
     * Secure platform storage is available.
     */
    SECURE_STORAGE,
}
