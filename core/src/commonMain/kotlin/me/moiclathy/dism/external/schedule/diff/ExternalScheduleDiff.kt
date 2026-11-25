package me.moiclathy.dism.external.schedule.diff

import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * 一次完整 External Schedule Diff。
 */
data class ExternalScheduleDiff(
    val scheduleId: ExternalScheduleId,

    val records: List<ExternalScheduleDiffRecord>,
) {

    val added:
        List<ExternalScheduleDiffRecord>
        get() =
            records.filter {
                it.type == ExternalScheduleDiffType.ADDED
            }

    val changed:
        List<ExternalScheduleDiffRecord>
        get() =
            records.filter {
                it.type == ExternalScheduleDiffType.CHANGED
            }

    val unchanged:
        List<ExternalScheduleDiffRecord>
        get() =
            records.filter {
                it.type == ExternalScheduleDiffType.UNCHANGED
            }

    val removed:
        List<ExternalScheduleDiffRecord>
        get() =
            records.filter {
                it.type == ExternalScheduleDiffType.REMOVED
            }

    val addedCount: Int
        get() = added.size

    val changedCount: Int
        get() = changed.size

    val unchangedCount: Int
        get() = unchanged.size

    val removedCount: Int
        get() = removed.size

    val hasChanges: Boolean
        get() =
            addedCount > 0 ||
                changedCount > 0 ||
                removedCount > 0
}
