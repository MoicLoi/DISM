package me.moiclathy.dism.external.schedule.e2e

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ExternalScheduleTimeFlowE2ERunnerTest {

    private fun createInput(
        eventSuccessful: Boolean = true,
        persistenceSuccessful: Boolean = true,
        recoverySuccessful: Boolean = true,
        timeFlowSuccessful: Boolean = true,
    ): ExternalScheduleTimeFlowE2EInput {

        return ExternalScheduleTimeFlowE2EInput(

            externalScheduleId =
                "schedule-001",

            externalEntryCount =
                3,

            normalizedEntryCount =
                3,

            eventDraftCount =
                3,

            createEvents = {

                if (eventSuccessful) {
                    ExternalScheduleE2EStageResult.success(
                        count = 3,
                        message =
                            "3 Events created",
                    )
                } else {
                    ExternalScheduleE2EStageResult.failure(
                        "Event creation failed"
                    )
                }
            },

            persist = {

                if (persistenceSuccessful) {
                    ExternalScheduleE2EStageResult.success(
                        count = 3,
                        message =
                            "3 records persisted",
                    )
                } else {
                    ExternalScheduleE2EStageResult.failure(
                        "Persistence failed"
                    )
                }
            },

            recover = {

                if (recoverySuccessful) {
                    ExternalScheduleE2EStageResult.success(
                        count = 3,
                        message =
                            "3 records recovered",
                    )
                } else {
                    ExternalScheduleE2EStageResult.failure(
                        "Recovery failed"
                    )
                }
            },

            processTimeFlow = {

                if (timeFlowSuccessful) {
                    ExternalScheduleE2EStageResult.success(
                        count = 3,
                        message =
                            "3 Events processed by time-flow",
                    )
                } else {
                    ExternalScheduleE2EStageResult.failure(
                        "Time-flow failed"
                    )
                }
            },
        )
    }

    @Test
    fun `full external schedule e2e succeeds`() {

        val runner =
            ExternalScheduleTimeFlowE2ERunner()

        val result =
            runner.run(
                createInput()
            )

        assertTrue(
            result.successful
        )

        assertNotNull(
            result.trace
        )

        assertEquals(
            3,
            result.trace!!.externalEntryCount
        )

        assertEquals(
            3,
            result.trace.normalizedEntryCount
        )

        assertEquals(
            3,
            result.trace.eventDraftCount
        )

        assertEquals(
            3,
            result.trace.persistedRecordCount
        )

        assertEquals(
            3,
            result.trace.recoveredRecordCount
        )

        assertEquals(
            3,
            result.trace.mappedEventCount
        )

        assertEquals(
            3,
            result.trace.timeFlowProcessedCount
        )
    }

    @Test
    fun `all required phases exist`() {

        val result =
            ExternalScheduleTimeFlowE2ERunner()
                .run(
                    createInput()
                )

        val phases =
            result.trace!!
                .steps
                .map {
                    it.phase
                }
                .toSet()

        assertEquals(
            ExternalScheduleTimeFlowE2EPhase
                .entries
                .toSet(),
            phases,
        )
    }

    @Test
    fun `final phase is validated correctly`() {

        val result =
            ExternalScheduleTimeFlowE2ERunner()
                .run(
                    createInput()
                )

        val finalStep =
            result.trace!!
                .steps
                .last()

        assertEquals(
            ExternalScheduleTimeFlowE2EPhase.FINAL,
            finalStep.phase
        )

        assertTrue(
            finalStep.successful
        )
    }

    @Test
    fun `event factory failure propagates to final result`() {

        val result =
            ExternalScheduleTimeFlowE2ERunner()
                .run(
                    createInput(
                        eventSuccessful = false
                    )
                )

        assertFalse(
            result.successful
        )

        val eventStep =
            result.trace!!
                .steps
                .first {
                    it.phase ==
                        ExternalScheduleTimeFlowE2EPhase
                            .EVENT_FACTORY
                }

        assertFalse(
            eventStep.successful
        )

        val finalStep =
            result.trace.steps.last()

        assertFalse(
            finalStep.successful
        )
    }

    @Test
    fun `persistence failure propagates to final result`() {

        val result =
            ExternalScheduleTimeFlowE2ERunner()
                .run(
                    createInput(
                        persistenceSuccessful = false
                    )
                )

        assertFalse(
            result.successful
        )

        assertFalse(
            result.trace!!
                .steps
                .first {
                    it.phase ==
                        ExternalScheduleTimeFlowE2EPhase
                            .PERSISTENCE
                }
                .successful
        )
    }

    @Test
    fun `recovery failure propagates to final result`() {

        val result =
            ExternalScheduleTimeFlowE2ERunner()
                .run(
                    createInput(
                        recoverySuccessful = false
                    )
                )

        assertFalse(
            result.successful
        )

        assertFalse(
            result.trace!!
                .steps
                .first {
                    it.phase ==
                        ExternalScheduleTimeFlowE2EPhase
                            .RECOVERY
                }
                .successful
        )
    }

    @Test
    fun `time flow failure propagates to final result`() {

        val result =
            ExternalScheduleTimeFlowE2ERunner()
                .run(
                    createInput(
                        timeFlowSuccessful = false
                    )
                )

        assertFalse(
            result.successful
        )

        assertFalse(
            result.trace!!
                .steps
                .first {
                    it.phase ==
                        ExternalScheduleTimeFlowE2EPhase
                            .TIME_FLOW
                }
                .successful
        )
    }

    @Test
    fun `runner is deterministic`() {

        val runner =
            ExternalScheduleTimeFlowE2ERunner()

        val first =
            runner.run(
                createInput()
            )

        val second =
            runner.run(
                createInput()
            )

        assertEquals(
            first,
            second
        )
    }

    @Test
    fun `contract rejects impossible counts`() {

        val trace =
            ExternalScheduleTimeFlowE2ETrace(

                steps =
                    ExternalScheduleTimeFlowE2EPhase
                        .entries
                        .map {
                            ExternalScheduleTimeFlowE2EStep(
                                phase = it,
                                successful = true,
                                message = "OK",
                            )
                        },

                externalScheduleId =
                    "schedule-001",

                externalEntryCount =
                    1,

                normalizedEntryCount =
                    2,

                eventDraftCount =
                    2,

                persistedRecordCount =
                    2,

                recoveredRecordCount =
                    2,

                mappedEventCount =
                    2,

                timeFlowProcessedCount =
                    2,
            )

        val errors =
            ExternalScheduleTimeFlowE2EContract
                .validate(trace)

        assertTrue(
            errors.isNotEmpty()
        )
    }
}
