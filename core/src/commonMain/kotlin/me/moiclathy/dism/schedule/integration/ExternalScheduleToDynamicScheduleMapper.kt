package me.moiclathy.dism.schedule.integration

import kotlin.time.Instant

import me.moiclathy.dism.external.schedule.ExternalSchedule
import me.moiclathy.dism.external.schedule.ExternalScheduleEntry
import me.moiclathy.dism.schedule.model.DynamicScheduleId
import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleItemType
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import me.moiclathy.dism.schedule.model.DynamicScheduleSource
import me.moiclathy.dism.schedule.model.DynamicScheduleState
import me.moiclathy.dism.schedule.model.DynamicScheduleTemporal

/**
 * 11.x External Schedule → 12.x Dynamic Schedule 的适配层。
 *
 * 这是产品化缝合的第一段：
 *
 * ```text
 * ICS / 喜鹊儿
 *      ↓
 * 11.x ExternalSchedule
 *      ↓ 本 mapper
 * 12.x DynamicScheduleModel
 * ```
 *
 * 注意：
 *
 * - 这里**不**重新解析 ICS，也不复制 11.x 的任何语义；
 * - 只做字段映射：时间区间 → Temporal，条目 → Schedule Item；
 * - state 先给 UPCOMING / UNSCHEDULED，
 *   由 12.2 State Projection 依据当前时间重新投影。
 */
class ExternalScheduleToDynamicScheduleMapper(
    private val policy:
        ExternalScheduleToDynamicSchedulePolicy =
        ExternalScheduleToDynamicSchedulePolicy(),
) {

    fun map(
        schedule: ExternalSchedule,
        mappedAt: Instant,
    ): DynamicScheduleModel {

        val scheduleId =
            DynamicScheduleId(
                schedule.id.value,
            )

        return DynamicScheduleModel(
            id = scheduleId,

            items =
                schedule.entries.map { entry ->
                    mapEntry(
                        scheduleId = scheduleId,
                        entry = entry,
                        mappedAt = mappedAt,
                    )
                },

            generatedAt = mappedAt,
        )
    }

    fun mapEntry(
        scheduleId: DynamicScheduleId,
        entry: ExternalScheduleEntry,
        mappedAt: Instant,
    ): DynamicScheduleItem {

        val temporal =
            temporalOf(entry)

        return DynamicScheduleItem(
            id =
                DynamicScheduleItemId(
                    entry.id.value,
                ),

            scheduleId = scheduleId,

            title = entry.title,

            type =
                if (entry.courseCode != null) {
                    DynamicScheduleItemType.EVENT
                } else {
                    DynamicScheduleItemType.TASK
                },

            source = DynamicScheduleSource.EXTERNAL,

            temporal = temporal,

            /*
             * 12.2 State Projection 会依据 now 重新计算，
             * 这里只给一个合法初值。
             */
            state =
                if (
                    temporal is
                    DynamicScheduleTemporal.Unscheduled
                ) {
                    DynamicScheduleState.UNSCHEDULED
                } else {
                    DynamicScheduleState.UPCOMING
                },

            priority = policy.defaultPriority,

            confidence = policy.defaultConfidence,

            externalKey = entry.id.value,

            createdAt = mappedAt,

            updatedAt = mappedAt,
        )
    }

    private fun temporalOf(
        entry: ExternalScheduleEntry,
    ): DynamicScheduleTemporal {

        val startAt = entry.startAt
        val endAt = entry.endAt

        return when {
            startAt != null && endAt != null ->
                DynamicScheduleTemporal.Interval(
                    startAt = startAt,
                    endAt = endAt,
                )

            startAt != null ->
                DynamicScheduleTemporal.At(
                    startAt = startAt,
                )

            else ->
                DynamicScheduleTemporal.Unscheduled
        }
    }
}

/**
 * 映射策略。
 */
data class ExternalScheduleToDynamicSchedulePolicy(
    val defaultPriority: Double = 0.6,

    val defaultConfidence: Double = 1.0,
) {
    init {
        require(defaultPriority in 0.0..1.0)
        require(defaultConfidence in 0.0..1.0)
    }
}
