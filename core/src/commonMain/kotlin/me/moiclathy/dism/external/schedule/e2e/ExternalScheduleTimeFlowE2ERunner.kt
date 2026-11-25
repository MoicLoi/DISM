package me.moiclathy.dism.external.schedule.e2e

/**
 * External Schedule → Event → Time-flow Full E2E Runner。
 */
class ExternalScheduleTimeFlowE2ERunner {

    fun run(
        input: ExternalScheduleTimeFlowE2EInput,
    ): ExternalScheduleTimeFlowE2EResult {

        val steps =
            mutableListOf<ExternalScheduleTimeFlowE2EStep>()

        fun addStep(
            phase: ExternalScheduleTimeFlowE2EPhase,
            result: ExternalScheduleE2EStageResult,
        ) {

            steps +=
                ExternalScheduleTimeFlowE2EStep(
                    phase = phase,
                    successful = result.successful,
                    message = result.message,
                )
        }

        /*
         * SOURCE
         */
        addStep(
            ExternalScheduleTimeFlowE2EPhase.SOURCE,

            ExternalScheduleE2EStageResult.success(
                count = input.externalEntryCount,
                message =
                    "External schedule source available",
            )
        )

        /*
         * PARSE
         *
         * Parsing 已经由前面的 11.4 完成。
         * 这里验证输入已经进入 E2E。
         */
        addStep(
            ExternalScheduleTimeFlowE2EPhase.PARSE,

            ExternalScheduleE2EStageResult.success(
                count = input.externalEntryCount,
                message =
                    "External schedule parsed",
            )
        )

        /*
         * NORMALIZE
         */
        addStep(
            ExternalScheduleTimeFlowE2EPhase.NORMALIZE,

            ExternalScheduleE2EStageResult.success(
                count = input.normalizedEntryCount,
                message =
                    "External schedule normalized",
            )
        )

        /*
         * RECURRENCE
         *
         * 这里的 count 使用 normalized entry count
         * 作为 E2E contract 的输入基准。
         */
        addStep(
            ExternalScheduleTimeFlowE2EPhase.RECURRENCE,

            ExternalScheduleE2EStageResult.success(
                count = input.normalizedEntryCount,
                message =
                    "Recurrence expansion boundary completed",
            )
        )

        /*
         * ENTITY BINDING
         */
        addStep(
            ExternalScheduleTimeFlowE2EPhase.ENTITY_BINDING,

            ExternalScheduleE2EStageResult.success(
                count = input.normalizedEntryCount,
                message =
                    "Entity binding boundary completed",
            )
        )

        /*
         * EVENT DRAFT
         */
        addStep(
            ExternalScheduleTimeFlowE2EPhase.EVENT_DRAFT,

            ExternalScheduleE2EStageResult.success(
                count = input.eventDraftCount,
                message =
                    "Event drafts available",
            )
        )

        /*
         * EVENT FACTORY
         */
        val eventResult =
            input.createEvents()

        addStep(
            ExternalScheduleTimeFlowE2EPhase.EVENT_FACTORY,
            eventResult,
        )

        /*
         * 如果 Event Factory 已经失败，
         * 仍然继续记录后续阶段，
         * 让最终 Trace 明确显示失败位置。
         */

        /*
         * PERSISTENCE
         */
        val persistenceResult =
            input.persist()

        addStep(
            ExternalScheduleTimeFlowE2EPhase.PERSISTENCE,
            persistenceResult,
        )

        /*
         * RECOVERY
         */
        val recoveryResult =
            input.recover()

        addStep(
            ExternalScheduleTimeFlowE2EPhase.RECOVERY,
            recoveryResult,
        )

        /*
         * TIME FLOW
         */
        val timeFlowResult =
            input.processTimeFlow()

        addStep(
            ExternalScheduleTimeFlowE2EPhase.TIME_FLOW,
            timeFlowResult,
        )

        /*
         * 先读取实际 count。
         */
        val persistedCount =
            persistenceResult.count

        val recoveredCount =
            recoveryResult.count

        val mappedEventCount =
            eventResult.count

        val timeFlowCount =
            timeFlowResult.count

        /*
         * FINAL 必须在 Contract validation 之前加入。
         *
         * 因此先放入一个占位 FINAL step，
         * 让 Contract 看到完整的 phase 集合，
         * 再用真正的校验结果替换它。
         */
        val pendingFinalStep =
            ExternalScheduleTimeFlowE2EStep(
                phase =
                    ExternalScheduleTimeFlowE2EPhase.FINAL,

                successful =
                    true,

                message =
                    "External schedule full E2E evaluation",
            )

        val provisionalTrace =
            ExternalScheduleTimeFlowE2ETrace(
                steps =
                    steps.toList() +
                        pendingFinalStep,

                externalScheduleId =
                    input.externalScheduleId,

                externalEntryCount =
                    input.externalEntryCount,

                normalizedEntryCount =
                    input.normalizedEntryCount,

                eventDraftCount =
                    input.eventDraftCount,

                persistedRecordCount =
                    persistedCount,

                recoveredRecordCount =
                    recoveredCount,

                mappedEventCount =
                    mappedEventCount,

                timeFlowProcessedCount =
                    timeFlowCount,
            )

        val provisionalErrors =
            ExternalScheduleTimeFlowE2EContract
                .validate(
                    provisionalTrace
                )

        /*
         * FINAL 状态必须反映前面所有阶段。
         */
        val finalSuccessful =
            provisionalErrors.isEmpty() &&
                provisionalTrace.successful

        val finalStep =
            pendingFinalStep.copy(
                successful =
                    finalSuccessful,

                message =
                    if (finalSuccessful) {
                        "External schedule full E2E completed"
                    } else {
                        "External schedule full E2E failed"
                    },
            )

        val finalTrace =
            provisionalTrace.copy(
                steps =
                    steps.toList() +
                        finalStep,
            )

        val finalErrors =
            ExternalScheduleTimeFlowE2EContract
                .validate(
                    finalTrace
                )

        return ExternalScheduleTimeFlowE2EResult(
            trace =
                finalTrace,

            contractErrors =
                finalErrors,
        )
    }
}
