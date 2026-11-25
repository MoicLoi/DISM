package me.moiclathy.dism.platform

import me.moiclathy.dism.api.Dism
import me.moiclathy.dism.api.DismInternalApi
import me.moiclathy.dism.config.DismConfiguration
import me.moiclathy.dism.persistence.DismPersistence

/**
 * Internal platform bootstrap contract.
 */
@DismInternalApi
interface DismPlatformBootstrap {

    val platform:
        DismPlatformInfo

    fun createPersistence(
        configuration:
            DismConfiguration,
    ): DismPersistence

    fun create(
        configuration:
            DismConfiguration,
    ): Dism
}
