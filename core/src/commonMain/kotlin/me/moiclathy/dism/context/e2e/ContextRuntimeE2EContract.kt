package me.moiclathy.dism.context.e2e

/**
 * Context Runtime E2E 不变量。
 *
 * 不负责执行，只负责描述必须成立的条件。
 */
object ContextRuntimeE2EContract {

    fun validate(
        trace: ContextRuntimeE2ETrace,
    ): List<String> {

        val failures =
            mutableListOf<String>()

        requirePhase(
            trace,
            ContextRuntimeE2EPhase.SOURCE,
            failures,
        )

        requirePhase(
            trace,
            ContextRuntimeE2EPhase.RESOLUTION,
            failures,
        )

        requirePhase(
            trace,
            ContextRuntimeE2EPhase.TRANSITION,
            failures,
        )

        requirePhase(
            trace,
            ContextRuntimeE2EPhase.CONFIDENCE,
            failures,
        )

        requirePhase(
            trace,
            ContextRuntimeE2EPhase.RUNTIME_EVENT,
            failures,
        )

        requirePhase(
            trace,
            ContextRuntimeE2EPhase.PERSISTED,
            failures,
        )

        requirePhase(
            trace,
            ContextRuntimeE2EPhase.RECOVERY,
            failures,
        )

        requirePhase(
            trace,
            ContextRuntimeE2EPhase.FINAL_STATE,
            failures,
        )

        val state =
            trace.recoveredState

        if (state == null) {
            failures +=
                "Recovered state must exist."
        }

        if (
            trace.events.isEmpty()
        ) {
            failures +=
                "At least one runtime event is required."
        }

        return failures
    }

    private fun requirePhase(
        trace: ContextRuntimeE2ETrace,
        phase: ContextRuntimeE2EPhase,
        failures: MutableList<String>,
    ) {

        if (!trace.contains(phase)) {
            failures +=
                "Missing E2E phase: $phase"
        }
    }
}
