package me.moiclathy.dism.e2e.scenario

import kotlin.test.Test
import kotlin.test.assertEquals

class DeterministicScenarioRunnerTest {
    @Test
    fun executes_steps_in_order_and_keeps_checkpoints() {
        val runner = DeterministicScenarioRunner<Int>()
        val result = runner.run(
            initialState = 0,
            steps = listOf(
                E2eScenarioStep("observation") { it + 1 },
                E2eScenarioStep("recommendation") { it * 10 },
                E2eScenarioStep("attention") { it + 5 },
            ),
        )

        assertEquals(15, result.finalState)
        assertEquals(listOf("observation", "recommendation", "attention"), ScenarioAssertions.checkpointNames(result))
        assertEquals(1, ScenarioAssertions.checkpointAt(result, 0))
        assertEquals(10, ScenarioAssertions.checkpointAt(result, 1))
    }

    @Test
    fun empty_scenario_is_identity() {
        val result = DeterministicScenarioRunner<String>().run("initial", emptyList())
        assertEquals("initial", result.initialState)
        assertEquals("initial", result.finalState)
        assertEquals(emptyList(), result.checkpoints)
    }

    @Test
    fun replaying_same_steps_is_deterministic() {
        val steps = listOf(
            E2eScenarioStep<Int>("a") { it + 2 },
            E2eScenarioStep<Int>("b") { it * 3 },
        )
        val runner = DeterministicScenarioRunner<Int>()
        val first = runner.run(1, steps)
        val second = runner.run(1, steps)
        assertEquals(first, second)
    }
}
