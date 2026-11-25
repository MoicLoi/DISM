package me.moiclathy.dism.external.schedule

import kotlin.time.Instant

/**
 * 一份外部日程。
 */
data class ExternalSchedule(
    val id: ExternalScheduleId,

    /**
     * 来源类型。
     */
    val source: ExternalScheduleOrigin,

    /**
     * 日程名称。
     */
    val name: String,

    /**
     * 本次导入 / 获取的时间。
     */
    val importedAt: Instant,

    /**
     * 外部日程条目。
     */
    val entries: List<ExternalScheduleEntry>,

    /**
     * 日程级元数据。
     */
    val metadata: ExternalScheduleMetadata =
        ExternalScheduleMetadata.EMPTY,
) {

    init {

        require(name.isNotBlank()) {
            "ExternalSchedule name must not be blank."
        }

        require(
            entries.all { it.source == source }
        ) {
            "All schedule entries must use the same source as the schedule."
        }

        require(
            entries
                .map { it.id }
                .distinct()
                .size == entries.size
        ) {
            "ExternalSchedule must not contain duplicate entry IDs."
        }
    }

    /**
     * 条目数量。
     */
    val size: Int
        get() =
            entries.size

    /**
     * 是否为空。
     */
    val isEmpty: Boolean
        get() =
            entries.isEmpty()

    /**
     * 是否非空。
     */
    val isNotEmpty: Boolean
        get() =
            entries.isNotEmpty()

    /**
     * 根据外部 ID 查找条目。
     */
    fun find(
        id: ExternalScheduleId,
    ): ExternalScheduleEntry? =
        entries.firstOrNull {
            it.id == id
        }

    /**
     * 返回按照开始时间排序的条目。
     *
     * 没有开始时间的条目放在最后。
     */
    fun sortedByStartTime():
        List<ExternalScheduleEntry> {

        return entries.sortedWith(
            compareBy(
                { it.startAt == null },
                { it.startAt },
                { it.id.value },
            )
        )
    }
}
