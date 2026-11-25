package me.moiclathy.dism.context.runtime

/**
 * Context Runtime 事件类型。
 */
enum class ContextRuntimeEventType {

    /**
     * Context 被观察到。
     */
    OBSERVED,

    /**
     * Context 被解析为当前 Context。
     */
    RESOLVED,

    /**
     * Context Transition。
     */
    TRANSITION,

    /**
     * Confidence 更新。
     */
    CONFIDENCE_CHANGED,

    /**
     * Context 过期。
     */
    EXPIRED,
}
