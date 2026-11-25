package me.moiclathy.dism.external.schedule.sync.idempotency

import kotlin.time.Instant

/**
 * 确定性的幂等状态机。
 *
 * 状态：
 *
 * NEW
 *   ↓ begin
 * IN_PROGRESS
 *   ├── complete → COMPLETED
 *   └── fail     → FAILED
 *
 * COMPLETED
 *   ↓ begin
 * 不重复执行
 *
 * FAILED
 *   ↓ begin
 * 可以重新获得执行权
 *
 * IN_PROGRESS
 *   ↓ begin
 * 默认不重复执行
 */
class DeterministicExternalScheduleSyncIdempotencyService(
    private val repository:
        ExternalScheduleSyncIdempotencyRepository,
) : ExternalScheduleSyncIdempotencyService {

    override fun begin(
        key:
            ExternalScheduleSyncIdempotencyKey,

        startedAt:
            Instant,
    ):
        ExternalScheduleSyncIdempotencyResult<Boolean> {

        val existing =
            repository.find(
                key
            )

        if (existing == null) {

            repository.save(

                ExternalScheduleSyncIdempotencyRecord(

                    key =
                        key,

                    status =
                        ExternalScheduleSyncIdempotencyStatus
                            .IN_PROGRESS,

                    startedAt =
                        startedAt,
                )
            )

            return ExternalScheduleSyncIdempotencyResult.success(
                value = true
            )
        }

        return when (
            existing.status
        ) {

            ExternalScheduleSyncIdempotencyStatus
                .NEW -> {

                repository.save(

                    existing.copy(
                        status =
                            ExternalScheduleSyncIdempotencyStatus
                                .IN_PROGRESS,

                        startedAt =
                            startedAt,

                        completedAt =
                            null,

                        message =
                            null,
                    )
                )

                ExternalScheduleSyncIdempotencyResult.success(
                    value = true
                )
            }

            ExternalScheduleSyncIdempotencyStatus
                .IN_PROGRESS -> {

                ExternalScheduleSyncIdempotencyResult.success(

                    value =
                        false,

                    reusedExistingResult =
                        true,

                    warnings =
                        listOf(
                            "Sync action is already in progress"
                        ),
                )
            }

            ExternalScheduleSyncIdempotencyStatus
                .COMPLETED -> {

                ExternalScheduleSyncIdempotencyResult.success(

                    value =
                        false,

                    reusedExistingResult =
                        true,

                    warnings =
                        listOf(
                            "Sync action already completed"
                        ),
                )
            }

            ExternalScheduleSyncIdempotencyStatus
                .FAILED -> {

                repository.save(

                    existing.copy(

                        status =
                            ExternalScheduleSyncIdempotencyStatus
                                .IN_PROGRESS,

                        startedAt =
                            startedAt,

                        completedAt =
                            null,

                        message =
                            null,
                    )
                )

                ExternalScheduleSyncIdempotencyResult.success(
                    value = true
                )
            }
        }
    }

    override fun complete(
        key:
            ExternalScheduleSyncIdempotencyKey,

        completedAt:
            Instant,

        eventId:
            String?,
    ):
        ExternalScheduleSyncIdempotencyResult<
            ExternalScheduleSyncIdempotencyRecord
        > {

        val existing =
            repository.find(
                key
            )

        if (existing == null) {

            return ExternalScheduleSyncIdempotencyResult.failure(

                errors =
                    listOf(
                        "Cannot complete unknown idempotency key"
                    )
            )
        }

        if (
            existing.status ==
                ExternalScheduleSyncIdempotencyStatus
                    .COMPLETED
        ) {

            return ExternalScheduleSyncIdempotencyResult.success(

                value =
                    existing,

                reusedExistingResult =
                    true,
            )
        }

        val completed =
            existing.copy(

                status =
                    ExternalScheduleSyncIdempotencyStatus
                        .COMPLETED,

                completedAt =
                    completedAt,

                eventId =
                    eventId,

                message =
                    null,
            )

        repository.save(
            completed
        )

        return ExternalScheduleSyncIdempotencyResult.success(
            value =
                completed
        )
    }

    override fun fail(
        key:
            ExternalScheduleSyncIdempotencyKey,

        completedAt:
            Instant,

        message:
            String,
    ):
        ExternalScheduleSyncIdempotencyResult<
            ExternalScheduleSyncIdempotencyRecord
        > {

        if (
            message.isBlank()
        ) {

            return ExternalScheduleSyncIdempotencyResult.failure(

                errors =
                    listOf(
                        "Failure message must not be blank"
                    )
            )
        }

        val existing =
            repository.find(
                key
            )

        if (existing == null) {

            return ExternalScheduleSyncIdempotencyResult.failure(

                errors =
                    listOf(
                        "Cannot fail unknown idempotency key"
                    )
            )
        }

        if (
            existing.status ==
                ExternalScheduleSyncIdempotencyStatus
                    .COMPLETED
        ) {

            return ExternalScheduleSyncIdempotencyResult.failure(

                errors =
                    listOf(
                        "Cannot overwrite completed sync action"
                    )
            )
        }

        val failed =
            existing.copy(

                status =
                    ExternalScheduleSyncIdempotencyStatus
                        .FAILED,

                completedAt =
                    completedAt,

                message =
                    message,
            )

        repository.save(
            failed
        )

        return ExternalScheduleSyncIdempotencyResult.success(
            value =
                failed
        )
    }

    override fun find(
        key:
            ExternalScheduleSyncIdempotencyKey,
    ):
        ExternalScheduleSyncIdempotencyRecord? {

        return repository.find(
            key
        )
    }
}
