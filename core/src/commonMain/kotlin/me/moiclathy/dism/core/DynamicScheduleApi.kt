package me.moiclathy.dism.core

/**
 * DISM Core API 分层。
 *
 * 13.1 不改变业务逻辑，
 * 只明确依赖方向。
 */
object DynamicScheduleApi {

    /**
     * Public API：
     *
     * App / Server / CLI 可以依赖。
     */
    const val PUBLIC_API = "public"

    /**
     * Internal API：
     *
     * Core 内部实现使用。
     */
    const val INTERNAL_API = "internal"

    /**
     * Persistence API：
     *
     * 数据持久化实现使用。
     */
    const val PERSISTENCE_API = "persistence"

    /**
     * Replay API：
     *
     * 历史决策分析使用。
     */
    const val REPLAY_API = "replay"
}
