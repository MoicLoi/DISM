package me.moiclathy.dism.event.duration

/**
 * Event Duration 的来源类型。
 */
enum class EventDurationType {

    /**
     * 明确知道的持续时间。
     *
     * 例如：
     *
     * “实验课持续 90 分钟”
     */
    FIXED,

    /**
     * 估算持续时间。
     *
     * 例如：
     *
     * “准备实验服大约 5 分钟”
     */
    ESTIMATED,

    /**
     * 由历史数据推导出来的持续时间。
     *
     * 例如：
     *
     * 最近 10 次洗漱平均需要 12 分钟。
     */
    DERIVED,
}
