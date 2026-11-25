package me.moiclathy.dism.provider

import kotlin.time.Instant
import me.moiclathy.dism.api.DismPublicApi

/**
 * Minimal context supplied to a provider operation.
 *
 * This deliberately does not expose DISM runtime internals.
 */
@DismPublicApi
data class DismProviderContext(
    val requestedAt: Instant,
    val attributes: Map<String, String> = emptyMap(),
)
