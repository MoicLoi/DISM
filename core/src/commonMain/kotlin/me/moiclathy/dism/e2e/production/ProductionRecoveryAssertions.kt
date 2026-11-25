package me.moiclathy.dism.e2e.production

object ProductionRecoveryAssertions {
    fun <T> assertDeterministic(first: T, second: T) {
        check(first == second) { "Production recovery cycle is not deterministic" }
    }

    fun assertPersistedEventCount(actual: Int, expected: Int) {
        check(actual == expected) { "Expected $expected persisted events, got $actual" }
    }
}
