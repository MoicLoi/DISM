package me.moiclathy.dism.e2e.production.contract

/** A deterministic trace of a production E2E scenario. */
data class ProductionE2ETrace(
    val phases: List<ProductionE2EPhase> = emptyList(),
    val checkpointNames: List<String> = emptyList(),
) {
    fun contains(phase: ProductionE2EPhase): Boolean = phase in phases

    fun reached(expected: List<ProductionE2EPhase>): Boolean =
        expected.indices.all { index -> phases.getOrNull(index) == expected[index] }
}
