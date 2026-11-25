package me.moiclathy.dism.context.resolution

import kotlin.time.Instant
import me.moiclathy.dism.context.source.ContextSourceResult

/**
 * Context Resolution 边界。
 *
 * Resolver：
 *
 * Sources
 *   ↓
 * Candidate aggregation
 *   ↓
 * conflict resolution
 *   ↓
 * ContextResolutionResult
 *
 * 但不负责应用 Context Transition。
 */
fun interface ContextResolver {

    fun resolve(
        sources: List<ContextSourceResult>,
        now: Instant,
        policy: ContextResolutionPolicy,
    ): ContextResolutionResult
}
