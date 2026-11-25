package me.moiclathy.dism.external.schedule.sync.execution

import kotlin.time.Instant

interface ExternalScheduleSyncActionExecutor {

    fun execute(
        request: ExternalScheduleSyncActionExecutionRequest,
        now: Instant,
    ): ExternalScheduleSyncActionExecutionResult
}
