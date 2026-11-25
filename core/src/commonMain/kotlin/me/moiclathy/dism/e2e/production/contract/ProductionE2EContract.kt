package me.moiclathy.dism.e2e.production.contract

/**
 * Invariants shared by production E2E fixtures.
 * This class is deliberately independent of concrete SemanticStateEngine and
 * SQLDelight constructors so it can be used as a contract at integration seams.
 */
object ProductionE2EContract {
    val requiredPhases: List<ProductionE2EPhase> = listOf(
        ProductionE2EPhase.OBSERVATION,
        ProductionE2EPhase.SEMANTIC_STATE,
        ProductionE2EPhase.RECOMMENDATION,
        ProductionE2EPhase.ATTENTION_DECISION,
        ProductionE2EPhase.PERSISTED,
        ProductionE2EPhase.FEEDBACK,
        ProductionE2EPhase.RECOVERY,
        ProductionE2EPhase.FINAL_STATE,
    )

    fun validate(trace: ProductionE2ETrace): List<String> = buildList {
        if (!trace.reached(requiredPhases)) {
            add("Production E2E phases must follow the canonical order")
        }
        if (trace.checkpointNames.distinct().size != trace.checkpointNames.size) {
            add("Checkpoint names must be unique")
        }
        if (trace.contains(ProductionE2EPhase.RECOVERY) &&
            !trace.contains(ProductionE2EPhase.PERSISTED)
        ) {
            add("Recovery requires a persisted phase")
        }
        if (trace.contains(ProductionE2EPhase.FINAL_STATE) &&
            !trace.contains(ProductionE2EPhase.RECOVERY)
        ) {
            add("Final state must be observed after recovery in this contract")
        }
    }

    fun isValid(trace: ProductionE2ETrace): Boolean = validate(trace).isEmpty()
}
