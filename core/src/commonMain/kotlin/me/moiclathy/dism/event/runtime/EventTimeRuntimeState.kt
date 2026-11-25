package me.moiclathy.dism.event.runtime

/**
 * Event Time Runtime 的运行状态。
 */
enum class EventTimeRuntimeState {

    /**
     * Runtime 尚未启动。
     */
    STOPPED,

    /**
     * Runtime 正常运行。
     */
    RUNNING,

    /**
     * Runtime 暂停。
     */
    PAUSED,
}
