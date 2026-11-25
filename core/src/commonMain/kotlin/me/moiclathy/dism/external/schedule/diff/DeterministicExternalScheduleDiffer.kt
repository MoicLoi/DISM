package me.moiclathy.dism.external.schedule.diff

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord

/**
 * 确定性的 External Schedule Diff。
 *
 * 不执行：
 *
 * - Event 修改
 * - Entity 修改
 * - State 修改
 * - Attention 修改
 * - 数据库写入
 */
class DeterministicExternalScheduleDiffer :
    ExternalScheduleDiffer {

    override fun diff(
        scheduleId: ExternalScheduleId,
        previous: List<ExternalSchedulePersistenceRecord>,
        current: List<ExternalSchedulePersistenceRecord>,
        policy: ExternalScheduleDiffPolicy,
    ): ExternalScheduleDiffResult {

        val errors =
            mutableListOf<String>()

        val previousDuplicateKeys =
            duplicateKeys(previous)

        if (previousDuplicateKeys.isNotEmpty()) {
            errors +=
                "Previous records contain duplicate keys: " +
                    previousDuplicateKeys.joinToString()
        }

        val currentDuplicateKeys =
            duplicateKeys(current)

        if (currentDuplicateKeys.isNotEmpty()) {
            errors +=
                "Current records contain duplicate keys: " +
                    currentDuplicateKeys.joinToString()
        }

        if (errors.isNotEmpty()) {
            return ExternalScheduleDiffResult.failure(
                errors
            )
        }

        val previousByKey =
            previous.associateBy {
                it.externalKey
            }

        val currentByKey =
            current.associateBy {
                it.externalKey
            }

        val allKeys =
            (previousByKey.keys + currentByKey.keys)
                .distinct()
                .sorted()

        val records =
            allKeys.map { key: String ->

                val oldRecord =
                    previousByKey[key]

                val newRecord =
                    currentByKey[key]

                when {

                    oldRecord == null &&
                        newRecord != null -> {

                        ExternalScheduleDiffRecord(
                            externalKey = key,

                            type =
                                ExternalScheduleDiffType.ADDED,

                            previous = null,

                            current = newRecord,
                        )
                    }

                    oldRecord != null &&
                        newRecord == null -> {

                        ExternalScheduleDiffRecord(
                            externalKey = key,

                            type =
                                ExternalScheduleDiffType.REMOVED,

                            previous = oldRecord,

                            current = null,
                        )
                    }

                    oldRecord != null &&
                        newRecord != null -> {

                        val changed =
                            hasMeaningfulChange(
                                previous = oldRecord,
                                current = newRecord,
                                policy = policy,
                            )

                        ExternalScheduleDiffRecord(
                            externalKey = key,

                            type =
                                if (changed) {
                                    ExternalScheduleDiffType.CHANGED
                                } else {
                                    ExternalScheduleDiffType.UNCHANGED
                                },

                            previous = oldRecord,

                            current = newRecord,
                        )
                    }

                    else -> {
                        error(
                            "Unexpected diff state for key=$key"
                        )
                    }
                }
            }

        return ExternalScheduleDiffResult.success(
            ExternalScheduleDiff(
                scheduleId = scheduleId,
                records = records,
            )
        )
    }

    private fun hasMeaningfulChange(
        previous:
            ExternalSchedulePersistenceRecord,

        current:
            ExternalSchedulePersistenceRecord,

        policy:
            ExternalScheduleDiffPolicy,
    ): Boolean {

        if (
            policy.compareTitle &&
            previous.title != current.title
        ) {
            return true
        }

        if (
            policy.compareStartAt &&
            previous.startAt != current.startAt
        ) {
            return true
        }

        if (
            policy.compareEndAt &&
            previous.endAt != current.endAt
        ) {
            return true
        }

        if (
            policy.compareLocationEntity &&
            previous.locationEntityId !=
                current.locationEntityId
        ) {
            return true
        }

        if (
            policy.compareInstructorEntity &&
            previous.instructorEntityId !=
                current.instructorEntityId
        ) {
            return true
        }

        if (
            policy.compareCourseEntity &&
            previous.courseEntityId !=
                current.courseEntityId
        ) {
            return true
        }

        if (
            policy.compareFingerprint &&
            previous.fingerprint !=
                current.fingerprint
        ) {
            return true
        }

        if (
            policy.compareActive &&
            previous.active != current.active
        ) {
            return true
        }

        return false
    }

    private fun duplicateKeys(
        records:
            List<ExternalSchedulePersistenceRecord>,
    ): Set<String> {

        return records
            .groupingBy {
                it.externalKey
            }
            .eachCount()
            .filterValues {
                it > 1
            }
            .keys
    }
}
