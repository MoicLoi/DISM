package me.moiclathy.dism.external.schedule.sync.persistence

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncRuntimeResult

/**
 * External Schedule Sync Persistence Service。
 *
 * 负责把一次 Sync Runtime 的执行结果
 * 转换为可持久化的同步历史。
 */
class ExternalScheduleSyncPersistenceService(
    private val repository:
        ExternalScheduleSyncPersistenceRepository,
) {

    /**
     * 持久化一次完整 Sync Runtime。
     */
    fun persist(
        result:
            ExternalScheduleSyncRuntimeResult,

        startedAt:
            Instant,

        completedAt:
            Instant,

        syncRunId:
            String,
    ): ExternalScheduleSyncPersistenceResult {

        if (
            syncRunId.isBlank()
        ) {

            return ExternalScheduleSyncPersistenceResult.failure(
                errors =
                    listOf(
                        "syncRunId must not be blank"
                    )
            )
        }

        if (
            completedAt < startedAt
        ) {

            return ExternalScheduleSyncPersistenceResult.failure(
                errors =
                    listOf(
                        "completedAt must not be before startedAt"
                    )
            )
        }

        val scheduleId =
            result.plan.scheduleId

        val records =
            result.executions.map {
                execution ->

                ExternalScheduleSyncPersistenceRecord(

                    scheduleId =
                        scheduleId,

                    externalKey =
                        execution.externalKey,

                    action =
                        execution.action,

                    status =
                        execution.status,

                    eventId =
                        execution.eventId,

                    executedAt =
                        completedAt,

                    message =
                        execution.message,

                    syncRunId =
                        syncRunId,
                )
            }

        return try {

            repository.saveAll(
                records
            )

            ExternalScheduleSyncPersistenceResult.success(
                records =
                    records,

                warnings =
                    result.warnings +
                        if (
                            result.errors.isNotEmpty()
                        ) {
                            listOf(
                                "Runtime completed with ${result.errors.size} error(s)"
                            )
                        } else {
                            emptyList()
                        },
            )

        } catch (e: Exception) {

            ExternalScheduleSyncPersistenceResult.failure(
                errors =
                    listOf(
                        e.message
                            ?: "Failed to persist sync result"
                    ),

                records =
                    records,
            )
        }
    }

    /**
     * 查询某个 Schedule 最近一次同步。
     */
    fun latestRun(
        scheduleId:
            ExternalScheduleId,
    ): List<ExternalScheduleSyncPersistenceRecord> {

        val runId =
            repository.findLatestRun(
                scheduleId
            )
                ?: return emptyList()

        return repository.findByRun(
            runId
        )
    }

    /**
     * 查询指定外部记录最近一次同步状态。
     */
    fun latestRecord(
        scheduleId:
            ExternalScheduleId,

        externalKey:
            String,
    ): ExternalScheduleSyncPersistenceRecord? {

        return repository.findLatest(
            scheduleId =
                scheduleId,

            externalKey =
                externalKey,
        )
    }
}
