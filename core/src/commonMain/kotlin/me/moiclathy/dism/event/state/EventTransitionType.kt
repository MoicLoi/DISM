package me.moiclathy.dism.event.state

/**
 * Event State 转换类型。
 */
enum class EventTransitionType {

    /**
     * 首次建立 Event State。
     */
    INITIALIZED,

    /**
     * 正常状态改变。
     */
    CHANGED,

    /**
     * 状态保持不变。
     *
     * 默认不产生这种 Transition。
     */
    PERSISTING,
}
