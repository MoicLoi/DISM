package me.moiclathy.dism.external.schedule.persistence

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * 一个外部课程/事件实例的持久化记录。
 *
 * 注意：
 * 这里保存的是 External Schedule 层面的事实，
 * 而不是完整 DISM Event。
 */
data class ExternalSchedulePersistenceRecord(
    val externalScheduleId: ExternalScheduleId,

    /**
     * ExternalScheduleEntry / occurrence 的稳定身份。
     */
    val externalKey: String,

    /**
     * 如果已经编译成 DISM Event，
     * 保存对应的内部 Event ID。
     *
     * 使用 String 是为了避免 Persistence 层
     * 强依赖 EventId 的具体实现。
     */
    val eventId: String?,

    val title: String,

    val startAt: Instant?,

    val endAt: Instant?,

    val locationEntityId: String? = null,

    val instructorEntityId: String? = null,

    val courseEntityId: String? = null,

    /**
     * 最后一次看到这个外部记录的时间。
     */
    val lastSeenAt: Instant,

    /**
     * 外部记录当前是否仍然存在。
     */
    val active: Boolean = true,

    /**
     * 原始数据版本/指纹。
     *
     * 用于未来 11.9 Diff。
     */
    val fingerprint: String? = null,
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

        require(lastSeenAt != Instant.DISTANT_PAST) {
            "lastSeenAt must be a meaningful timestamp"
        }
    }
}
