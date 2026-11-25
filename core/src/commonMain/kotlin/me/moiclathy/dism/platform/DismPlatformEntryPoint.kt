package me.moiclathy.dism.platform

import me.moiclathy.dism.api.Dism
import me.moiclathy.dism.api.DismPublicApi
import me.moiclathy.dism.config.DismConfiguration

/**
 * Platform-specific DISM entry point.
 *
 * The implementation is responsible for resolving platform
 * infrastructure while keeping the resulting DISM instance
 * behind the same public API.
 */
@DismPublicApi
interface DismPlatformEntryPoint {

    /**
     * Platform information.
     */
    val platform:
        DismPlatformInfo

    /**
     * Create a DISM runtime using platform defaults.
     */
    fun create(
        configuration:
            DismConfiguration,
    ): Dism
}
