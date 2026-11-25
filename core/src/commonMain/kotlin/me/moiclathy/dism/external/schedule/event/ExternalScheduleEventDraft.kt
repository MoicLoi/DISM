package me.moiclathy.dism.external.schedule.event

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.ExternalScheduleEntry

/**
 * 外部课程数据编译成 DISM Event 前的中间表示。
 *
 * 注意：
 * 这里故意不直接依赖 domain.event.Event。
 *
 * 它是：
 *
 * ExternalScheduleEntry
 *        ↓
 * ExternalScheduleEventDraft
 *        ↓
 * EventFactory / Event Adapter
 *        ↓
 * Event
 *
 * 这样 External Schedule 模块不会反向绑定 Event 的具体实现。
 */
data class ExternalScheduleEventDraft(
    val sourceEntryId: ExternalScheduleId,

    /**
     * 稳定的外部事件键。
     *
     * 同一个外部课程实例重新导入时，
     * 应尽可能生成相同的 key。
     */
    val externalKey: String,

    val title: String,

    val startAt: Instant?,

    val endAt: Instant?,

    val location: String?,

    val instructor: String?,

    val courseCode: String?,

    val description: String?,

    /**
     * 外部数据的原始 metadata。
     */
    val metadata: Map<String, String>,
) {

    init {
        require(externalKey.isNotBlank()) {
            "externalKey must not be blank"
        }

        require(title.isNotBlank()) {
            "title must not be blank"
        }

        if (startAt != null && endAt != null) {
            require(endAt >= startAt) {
                "endAt must not be earlier than startAt"
            }
        }
    }

    /**
     * 是否具有确定的时间。
     */
    val hasTemporalInformation: Boolean
        get() = startAt != null

    /**
     * 是否具有完整时间区间。
     */
    val hasTimeRange: Boolean
        get() = startAt != null && endAt != null
}
