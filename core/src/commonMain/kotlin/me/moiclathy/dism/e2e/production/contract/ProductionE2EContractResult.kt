package me.moiclathy.dism.e2e.production.contract

data class ProductionE2EContractResult(
    val valid: Boolean,
    val violations: List<String> = emptyList(),
) {
    companion object {
        fun from(trace: ProductionE2ETrace): ProductionE2EContractResult {
            val violations = ProductionE2EContract.validate(trace)
            return ProductionE2EContractResult(
                valid = violations.isEmpty(),
                violations = violations,
            )
        }
    }
}
