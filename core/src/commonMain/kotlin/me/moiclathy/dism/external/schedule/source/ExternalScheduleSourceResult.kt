package me.moiclathy.dism.external.schedule.source

import me.moiclathy.dism.external.schedule.ExternalSchedule

/**
 * 外部日程来源获取结果。
 */
data class ExternalScheduleSourceResult(
    val sourceId: ExternalScheduleSourceId,
    val schedule: ExternalSchedule?,
    val fetchedAt: kotlin.time.Instant,
    val success: Boolean,
    val error: String? = null,
    val metadata: Map<String, String> = emptyMap(),
) {

    init {

        require(
            if (success) {
                schedule != null &&
                    error == null
            } else {
                schedule == null &&
                    !error.isNullOrBlank()
            }
        ) {
            "Source result must be internally consistent."
        }

        require(
            metadata.keys.all { it.isNotBlank() }
        ) {
            "Result metadata keys must not be blank."
        }
    }

    companion object {

        fun success(
            sourceId: ExternalScheduleSourceId,
            schedule: ExternalSchedule,
            fetchedAt: kotlin.time.Instant,
            metadata: Map<String, String> = emptyMap(),
        ): ExternalScheduleSourceResult {

            return ExternalScheduleSourceResult(
                sourceId =
                    sourceId,

                schedule =
                    schedule,

                fetchedAt =
                    fetchedAt,

                success =
                    true,

                error =
                    null,

                metadata =
                    metadata,
            )
        }

        fun failure(
            sourceId: ExternalScheduleSourceId,
            fetchedAt: kotlin.time.Instant,
            error: String,
            metadata: Map<String, String> = emptyMap(),
        ): ExternalScheduleSourceResult {

            require(error.isNotBlank()) {
                "Source failure error must not be blank."
            }

            return ExternalScheduleSourceResult(
                sourceId =
                    sourceId,

                schedule =
                    null,

                fetchedAt =
                    fetchedAt,

                success =
                    false,

                error =
                    error,

                metadata =
                    metadata,
            )
        }
    }
}
