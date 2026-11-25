package me.moiclathy.dism.semantic.resolution

import kotlin.uuid.Uuid

/**
 * Entity Resolution 的上下文信息。
 *
 * 提供当前正在发生的事件、即将发生的事件，
 * 以及对当前语义解析特别相关的实体提示。
 *
 * 设计原则：
 * - State / Semantic Engine 负责生成 contextual hints
 * - EntityResolver 只负责利用 hints 做 resolution
 * - Resolver 不应该自己变成半个 World Model
 */
data class ResolutionContext(
    /**
     * 当前正在发生的事件。
     */
    val activeEventIds: Set<Uuid> = emptySet(),

    /**
     * 即将发生的事件。
     */
    val upcomingEventIds: Set<Uuid> = emptySet(),

    /**
     * 对当前语义解析特别相关的实体。
     *
     * 第一版先由上层构造。
     */
    val entityHints: List<ContextEntityHint> = emptyList(),
)
