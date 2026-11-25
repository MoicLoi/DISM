package me.moiclathy.dism.context.source

import me.moiclathy.dism.context.model.ContextClaim

/**
 * Context 的来源。
 *
 * 一个 Source 可以根据当前输入产生零个或多个 ContextClaim。
 *
 * Source 只负责"提供候选事实"，
 * 不负责解决不同 Source 之间的冲突。
 */
interface ContextSource {

    /**
     * 稳定的 Source ID。
     */
    val id: ContextSourceId

    /**
     * Source 的来源类型。
     */
    val type: ContextSourceType

    /**
     * 根据输入产生 ContextClaim。
     *
     * 不允许在这里直接修改 ContextState。
     */
    fun collect(
        input: ContextSourceInput,
    ): ContextSourceResult
}
