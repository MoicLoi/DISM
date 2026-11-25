package me.moiclathy.dism.external.schedule.e2e

/**
 * 11.12 Full E2E 的执行边界。
 *
 * 每个函数代表一层真实系统。
 *
 * 11.12 本身不重新实现这些系统。
 */
data class ExternalScheduleTimeFlowE2EInput(
    val externalScheduleId: String,

    val externalEntryCount: Int,

    val normalizedEntryCount: Int,

    val eventDraftCount: Int,

    val persist: () -> ExternalScheduleE2EStageResult,

    val recover: () -> ExternalScheduleE2EStageResult,

    val createEvents: () -> ExternalScheduleE2EStageResult,

    val processTimeFlow: () -> ExternalScheduleE2EStageResult,
) {

    init {
        require(externalScheduleId.isNotBlank()) {
            "externalScheduleId must not be blank"
        }

        require(externalEntryCount >= 0) {
            "externalEntryCount must not be negative"
        }

        require(normalizedEntryCount >= 0) {
            "normalizedEntryCount must not be negative"
        }

        require(eventDraftCount >= 0) {
            "eventDraftCount must not be negative"
        }
    }
}

/**
 * E2E 某一阶段的最小结果。
 *
 * count 表示该阶段产生/处理的对象数量。
 */
data class ExternalScheduleE2EStageResult(
    val successful: Boolean,
    val count: Int,
    val message: String,
) {

    init {
        require(count >= 0) {
            "count must not be negative"
        }

        require(message.isNotBlank()) {
            "message must not be blank"
        }
    }

    companion object {

        fun success(
            count: Int,
            message: String,
        ): ExternalScheduleE2EStageResult =
            ExternalScheduleE2EStageResult(
                successful = true,
                count = count,
                message = message,
            )

        fun failure(
            message: String,
        ): ExternalScheduleE2EStageResult =
            ExternalScheduleE2EStageResult(
                successful = false,
                count = 0,
                message = message,
            )
    }
}
