package me.moiclathy.dism.e2e.production

/**
 * Deterministic production-style lifecycle for a semantic/attention cycle.
 * The orchestration is deliberately dependency-free; concrete persistence and
 * runtime implementations are supplied by adapters from the host application.
 */
class ProductionRecoveryCycle<S, R, E, F, O>(
    private val evaluate: (S) -> R,
    private val persist: (R) -> E,
    private val applyFeedback: (R, F) -> R,
    private val recover: (List<E>) -> O,
    private val snapshot: (R) -> S,
) {
    fun run(initial: S, feedback: F): CycleResult<R, E, O, S> {
        val evaluated = evaluate(initial)
        val event = persist(evaluated)
        val updated = applyFeedback(evaluated, feedback)
        val updatedEvent = persist(updated)
        val recovered = recover(listOf(event, updatedEvent))
        return CycleResult(evaluated, updated, listOf(event, updatedEvent), recovered, snapshot(updated))
    }
}

data class CycleResult<R, E, O, S>(
    val initialResult: R,
    val feedbackResult: R,
    val persistedEvents: List<E>,
    val recoveredState: O,
    val nextSnapshot: S,
)
