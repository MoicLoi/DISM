package me.moiclathy.dism.external.schedule.event

import me.moiclathy.dism.external.schedule.ExternalSchedule
import me.moiclathy.dism.external.schedule.ExternalScheduleEntry

/**
 * 确定性的 External Schedule → Event Draft 编译器。
 *
 * 特点：
 *
 * - 不使用 LLM
 * - 不做语义推理
 * - 不修改 Entity
 * - 不修改 Relation
 * - 不修改 Attention
 * - 不修改 Event Runtime
 * - 相同输入产生相同输出
 */
class DeterministicExternalScheduleEventCompiler(
    private val defaultPolicy: ExternalScheduleEventCompilationPolicy =
        ExternalScheduleEventCompilationPolicy(),
) : ExternalScheduleEventCompiler {

    override fun compile(
        schedule: ExternalSchedule,
        policy: ExternalScheduleEventCompilationPolicy,
    ): ExternalScheduleEventCompilationResult {

        val drafts = mutableListOf<ExternalScheduleEventDraft>()
        val errors = mutableListOf<ExternalScheduleEventCompilationError>()
        val warnings = mutableListOf<String>()

        for (entry in schedule.entries) {

            val result = compileEntry(
                entry = entry,
                policy = policy,
            )

            drafts += result.drafts
            errors += result.errors
            warnings += result.warnings
        }

        return if (errors.isEmpty()) {
            ExternalScheduleEventCompilationResult.success(
                drafts = drafts,
                warnings = warnings,
            )
        } else {
            ExternalScheduleEventCompilationResult(
                drafts = drafts,
                errors = errors,
                warnings = warnings,
            )
        }
    }

    override fun compileEntry(
        entry: ExternalScheduleEntry,
        policy: ExternalScheduleEventCompilationPolicy,
    ): ExternalScheduleEventCompilationResult {

        val errors = mutableListOf<ExternalScheduleEventCompilationError>()
        val warnings = mutableListOf<String>()

        val sourceEntryId = entry.id.value

        if (sourceEntryId.isBlank()) {
            errors += ExternalScheduleEventCompilationError(
                code = ExternalScheduleEventCompilationError.Code.INVALID_SOURCE_ID,
                message = "External schedule entry id must not be blank",
                sourceEntryId = sourceEntryId,
            )

            return ExternalScheduleEventCompilationResult.failure(
                errors = errors,
                warnings = warnings,
            )
        }

        /*
         * ---------------------------------------------------------
         * Title
         * ---------------------------------------------------------
         */

        val title = entry.title
            .trim()
            .takeIf { it.isNotEmpty() }

        val resolvedTitle = when {
            title != null -> title

            policy.allowFallbackTitle -> {
                warnings +=
                    "Entry $sourceEntryId has no title; source id was used as fallback title"

                sourceEntryId
            }

            else -> {
                errors += ExternalScheduleEventCompilationError(
                    code = ExternalScheduleEventCompilationError.Code.MISSING_TITLE,
                    message = "External schedule entry has no usable title",
                    sourceEntryId = sourceEntryId,
                )

                null
            }
        }

        if (resolvedTitle == null) {
            return ExternalScheduleEventCompilationResult.failure(
                errors = errors,
                warnings = warnings,
            )
        }

        /*
         * ---------------------------------------------------------
         * Temporal information
         * ---------------------------------------------------------
         */

        val startAt = entry.startAt
        val endAt = entry.endAt

        if (startAt == null && !policy.allowUnscheduled) {
            errors += ExternalScheduleEventCompilationError(
                code =
                    ExternalScheduleEventCompilationError.Code.UNSCHEDULED_NOT_ALLOWED,
                message =
                    "Entry $sourceEntryId has no start time and policy does not allow unscheduled events",
                sourceEntryId = sourceEntryId,
            )
        }

        if (
            startAt != null &&
            endAt != null &&
            endAt < startAt
        ) {
            if (policy.rejectInvalidTimeRange) {

                errors += ExternalScheduleEventCompilationError(
                    code =
                        ExternalScheduleEventCompilationError.Code.INVALID_TIME_RANGE,
                    message =
                        "Entry $sourceEntryId has an end time earlier than its start time",
                    sourceEntryId = sourceEntryId,
                )

            } else {

                warnings +=
                    "Entry $sourceEntryId has an invalid time range; end time was ignored"
            }
        }

        if (errors.isNotEmpty()) {
            return ExternalScheduleEventCompilationResult.failure(
                errors = errors,
                warnings = warnings,
            )
        }

        /*
         * ---------------------------------------------------------
         * Stable external key
         * ---------------------------------------------------------
         *
         * 注意：
         * 这里不使用随机 UUID。
         *
         * 外部课程重复导入时，
         * 相同 entry id 应产生相同 key。
         */

        val externalKey =
            buildExternalKey(entry)

        if (externalKey.isBlank()) {

            errors += ExternalScheduleEventCompilationError(
                code =
                    ExternalScheduleEventCompilationError.Code.INVALID_EXTERNAL_KEY,
                message =
                    "Unable to build stable external key",
                sourceEntryId = sourceEntryId,
            )

            return ExternalScheduleEventCompilationResult.failure(
                errors = errors,
                warnings = warnings,
            )
        }

        /*
         * ---------------------------------------------------------
         * Metadata
         * ---------------------------------------------------------
         */

        val metadata: Map<String, String> =
            if (policy.preserveMetadata) {
                entry.metadata.values
            } else {
                emptyMap()
            }

        /*
         * ---------------------------------------------------------
         * Draft
         * ---------------------------------------------------------
         */

        val draft = ExternalScheduleEventDraft(
            sourceEntryId = entry.id,

            externalKey = externalKey,

            title = resolvedTitle,

            startAt = startAt,

            endAt =
                if (
                    startAt != null &&
                    endAt != null &&
                    endAt >= startAt
                ) {
                    endAt
                } else {
                    null
                },

            location = entry.location
                ?.trim()
                ?.takeIf { it.isNotEmpty() },

            instructor = entry.instructor
                ?.trim()
                ?.takeIf { it.isNotEmpty() },

            courseCode = entry.courseCode
                ?.trim()
                ?.takeIf { it.isNotEmpty() },

            description = entry.description
                ?.trim()
                ?.takeIf { it.isNotEmpty() },

            metadata = metadata,
        )

        return ExternalScheduleEventCompilationResult.success(
            drafts = listOf(draft),
            warnings = warnings,
        )
    }

    private fun buildExternalKey(
        entry: ExternalScheduleEntry,
    ): String {

        /*
         * 第一优先级：
         *
         * ExternalScheduleEntry.id
         *
         * 因为 UID 通常来自 ICS UID，
         * 或者来自前面的外部 Schedule Adapter。
         */

        return buildString {
            append(entry.source.name.lowercase())
            append(":")
            append(entry.id.value)
        }
    }
}
