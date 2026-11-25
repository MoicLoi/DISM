package me.moiclathy.dism.external.schedule.event

/**
 * 对已有 Event Factory 的统一适配边界。
 *
 * 11.9 本身不实现 Event Domain。
 *
 * 具体项目只需要把现有：
 *
 * EventFactory
 *
 * 包装成：
 *
 * ExternalScheduleEventFactory<T>
 *
 * 即可。
 */
class ExternalScheduleEventFactoryAdapter<T>(
    private val policy:
        ExternalScheduleEventFactoryPolicy =
        ExternalScheduleEventFactoryPolicy(),
    private val factory:
        (ExternalScheduleEventFactoryInput) ->
            T?,
) : ExternalScheduleEventFactory<T> {

    override fun create(
        input: ExternalScheduleEventFactoryInput,
    ): ExternalScheduleEventFactoryResult<T> {

        val draft =
            input.draft

        val warnings =
            mutableListOf<String>()

        /*
         * ---------------------------------------------------------
         * externalKey
         * ---------------------------------------------------------
         */

        if (
            policy.requireExternalKey &&
            draft.externalKey.isBlank()
        ) {

            return ExternalScheduleEventFactoryResult.failure(
                errors =
                    listOf(
                        ExternalScheduleEventFactoryError(
                            code =
                                ExternalScheduleEventFactoryError.Code
                                    .INVALID_EXTERNAL_KEY,

                            message =
                                "External event key must not be blank",
                        )
                    )
            )
        }

        /*
         * ---------------------------------------------------------
         * temporal validation
         * ---------------------------------------------------------
         */

        if (
            draft.startAt == null &&
            !policy.allowUnscheduled
        ) {

            return ExternalScheduleEventFactoryResult.failure(
                externalKey =
                    draft.externalKey,

                errors =
                    listOf(
                        ExternalScheduleEventFactoryError(
                            code =
                                ExternalScheduleEventFactoryError.Code
                                    .POLICY_REJECTED,

                            message =
                                "Unscheduled external event is not allowed",

                            externalKey =
                                draft.externalKey,
                        )
                    )
            )
        }

        if (
            draft.startAt != null &&
            draft.endAt == null &&
            !policy.allowMissingEndTime
        ) {

            return ExternalScheduleEventFactoryResult.failure(
                externalKey =
                    draft.externalKey,

                errors =
                    listOf(
                        ExternalScheduleEventFactoryError(
                            code =
                                ExternalScheduleEventFactoryError.Code
                                    .POLICY_REJECTED,

                            message =
                                "Event without end time is not allowed",

                            externalKey =
                                draft.externalKey,
                        )
                    )
            )
        }

        /*
         * ---------------------------------------------------------
         * Event creation
         * ---------------------------------------------------------
         */

        val event =
            try {
                factory(input)
            } catch (exception: IllegalArgumentException) {

                return ExternalScheduleEventFactoryResult.failure(
                    externalKey =
                        draft.externalKey,

                    errors =
                        listOf(
                            ExternalScheduleEventFactoryError(
                                code =
                                    ExternalScheduleEventFactoryError.Code
                                        .INVALID_DRAFT,

                                message =
                                    exception.message
                                        ?: "Event factory rejected draft",

                                externalKey =
                                    draft.externalKey,
                            )
                        )
                )

            } catch (exception: Exception) {

                return ExternalScheduleEventFactoryResult.failure(
                    externalKey =
                        draft.externalKey,

                    errors =
                        listOf(
                            ExternalScheduleEventFactoryError(
                                code =
                                    ExternalScheduleEventFactoryError.Code
                                        .INTERNAL_ERROR,

                                message =
                                    exception.message
                                        ?: "Unexpected Event factory error",

                                externalKey =
                                    draft.externalKey,
                            )
                        )
                )
            }

        if (event == null) {

            return ExternalScheduleEventFactoryResult.failure(
                externalKey =
                    draft.externalKey,

                errors =
                    listOf(
                        ExternalScheduleEventFactoryError(
                            code =
                                ExternalScheduleEventFactoryError.Code
                                    .FACTORY_REJECTED,

                            message =
                                "Event factory returned null",

                            externalKey =
                                draft.externalKey,
                        )
                    )
            )
        }

        /*
         * ---------------------------------------------------------
         * warnings
         * ---------------------------------------------------------
         */

        if (draft.startAt == null) {

            warnings +=
                "External event '${draft.externalKey}' is unscheduled"
        }

        if (
            draft.startAt != null &&
            draft.endAt == null
        ) {

            warnings +=
                "External event '${draft.externalKey}' has no end time"
        }

        return ExternalScheduleEventFactoryResult.success(
            event =
                event,

            externalKey =
                draft.externalKey,

            warnings =
                warnings,
        )
    }
}
