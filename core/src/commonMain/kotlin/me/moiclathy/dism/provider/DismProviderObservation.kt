package me.moiclathy.dism.provider

import kotlin.time.Instant
import me.moiclathy.dism.api.DismPublicApi

/**
 * Observation produced by an external provider.
 *
 * This is intentionally source-oriented.
 * It is not itself a DISM Entity or Relation.
 */
@DismPublicApi
data class DismProviderObservation(
    val providerId: String,
    val observedAt: Instant,
    val externalKey: String? = null,
    val payload: Map<String, String> = emptyMap(),
)
