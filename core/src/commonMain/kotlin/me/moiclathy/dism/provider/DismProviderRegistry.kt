package me.moiclathy.dism.provider

import me.moiclathy.dism.api.DismPublicApi

/**
 * Registry of providers available to the application.
 */
@DismPublicApi
interface DismProviderRegistry {

    fun register(
        provider: DismProvider,
    )

    fun unregister(
        providerId: String,
    )

    fun find(
        providerId: String,
    ): DismProvider?

    fun all():
        List<DismProvider>
}
