package me.moiclathy.dism.external.schedule.diff

/**
 * 两次 External Schedule 导入之间的变化类型。
 */
enum class ExternalScheduleDiffType {

    /**
     * 外部记录第一次出现。
     */
    ADDED,

    /**
     * 外部记录仍然存在，但内容发生变化。
     */
    CHANGED,

    /**
     * 外部记录仍然存在，而且内容没有变化。
     */
    UNCHANGED,

    /**
     * 旧记录在新的 External Schedule 中消失。
     */
    REMOVED,
}
