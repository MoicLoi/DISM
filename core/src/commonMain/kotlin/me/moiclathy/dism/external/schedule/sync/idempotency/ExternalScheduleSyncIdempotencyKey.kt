package me.moiclathy.dism.external.schedule.sync.idempotency

import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * 一次 Sync Action 的稳定幂等键。
 *
 * 同一个：
 *
 * scheduleId
 * + externalKey
 * + syncRunId
 * + action
 *
 * 应当只对应一次逻辑执行。
 */
data class ExternalScheduleSyncIdempotencyKey(

    val scheduleId:
        ExternalScheduleId,

    val externalKey:
        String,

    val syncRunId:
        String,

    val action:
        String,
) {

    init {

        require(
            externalKey.isNotBlank()
        ) {
            "externalKey must not be blank"
        }

        require(
            syncRunId.isNotBlank()
        ) {
            "syncRunId must not be blank"
        }

        require(
            action.isNotBlank()
        ) {
            "action must not be blank"
        }
    }

    /**
     * 用于持久化或日志的稳定字符串。
     */
    val value: String
        get() =
            buildString {

                append(scheduleId.value)
                append(":")
                append(externalKey)
                append(":")
                append(syncRunId)
                append(":")
                append(action)
            }
}
