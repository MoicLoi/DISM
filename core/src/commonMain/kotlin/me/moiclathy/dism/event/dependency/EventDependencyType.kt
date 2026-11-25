package me.moiclathy.dism.event.dependency

/**
 * Event 之间的依赖类型。
 *
 * 注意：
 * Dependency 描述的是“事件之间存在什么关系”，
 * 不负责计算具体时间。
 */
enum class EventDependencyType {

    /**
     * 当前 Event 必须等待另一个 Event 完成。
     *
     * A depends on B:
     *
     * B → A
     */
    REQUIRES_COMPLETION,

    /**
     * 当前 Event 不能早于另一个 Event 开始。
     *
     * B starts → A may start
     */
    REQUIRES_START,

    /**
     * 当前 Event 与另一个 Event 存在顺序关系。
     *
     * B → A
     */
    PRECEDES,

    /**
     * 两个 Event 应保持关联。
     *
     * 不表达严格的时间先后。
     */
    ASSOCIATED,
}
