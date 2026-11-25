package me.moiclathy.dism.context.transition

/**
 * Context 状态转移类型。
 *
 * 注意：
 * 这里描述的是"状态变化"，
 * 而不是 Context 本身。
 */
enum class ContextTransitionType {

    /**
     * 没有 Context → 进入某个 Context。
     */
    ENTERED,

    /**
     * 从某个 Context → 另一个 Context。
     *
     * 例如：
     * IN_TRANSIT → IN_CLASS
     */
    CHANGED,

    /**
     * 当前 Context 被明确退出，
     * 且没有立即进入新的 Context。
     */
    EXITED,

    /**
     * 当前 Context 因 expiresAt 等原因失效。
     */
    EXPIRED,

    /**
     * Context 保持不变。
     *
     * 主要用于 Runtime / Event Log。
     */
    PERSISTING,
}
