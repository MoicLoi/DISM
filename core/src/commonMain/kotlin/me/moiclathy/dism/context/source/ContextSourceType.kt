package me.moiclathy.dism.context.source

/**
 * 描述 ContextClaim 的来源类型。
 *
 * SourceType 只回答：
 * "这个上下文信息是从哪里来的？"
 *
 * 它不负责判断这个信息是否正确。
 */
enum class ContextSourceType {

    /**
     * 用户明确表达。
     *
     * 例如：
     * "我现在在上课。"
     */
    USER_INPUT,

    /**
     * 来自日程、课程表、事件计划等外部计划源。
     *
     * 例如：
     * 当前时间与课程表匹配，因此候选上下文为 IN_CLASS。
     */
    SCHEDULE,

    /**
     * 来自系统直接观察到的状态。
     *
     * 例如未来接入设备、应用状态或其他本地观察源。
     */
    OBSERVATION,

    /**
     * 由已有语义状态推导得到。
     *
     * 例如：
     * EVENT + currentTime
     *      -> IN_CLASS
     */
    DERIVED,

    /**
     * 系统内部产生的上下文。
     */
    SYSTEM
}
