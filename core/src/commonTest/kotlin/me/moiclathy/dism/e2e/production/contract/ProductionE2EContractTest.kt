package me.moiclathy.dism.e2e.production.contract

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProductionE2EContractTest {
    @Test
    fun canonicalTraceIsValid() {
        val trace = ProductionE2ETrace(
            phases = ProductionE2EContract.requiredPhases,
            checkpointNames = ProductionE2EContract.requiredPhases.map { it.name.lowercase() },
        )

        assertTrue(ProductionE2EContract.isValid(trace))
        assertTrue(ProductionE2EContractResult.from(trace).valid)
    }

    @Test
    fun recoveryWithoutPersistenceIsRejected() {
        val phases = listOf(
            ProductionE2EPhase.OBSERVATION,
            ProductionE2EPhase.SEMANTIC_STATE,
            ProductionE2EPhase.RECOVERY,
        )

        assertFalse(ProductionE2EContract.isValid(ProductionE2ETrace(phases)))
    }

    @Test
    fun duplicateCheckpointNamesAreRejected() {
        val trace = ProductionE2ETrace(
            phases = ProductionE2EContract.requiredPhases,
            checkpointNames = listOf("observation", "observation"),
        )

        assertFalse(ProductionE2EContract.isValid(trace))
    }
}
