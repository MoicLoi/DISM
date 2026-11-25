package me.moiclathy.dism.state.temporal

/**
 * 一次时间线重建产生的具体变更集合。
 */
data class TimelineChangeSet(
    val changes: List<StateChange>,
) {
    val isEmpty: Boolean
        get() = changes.isEmpty()
}
