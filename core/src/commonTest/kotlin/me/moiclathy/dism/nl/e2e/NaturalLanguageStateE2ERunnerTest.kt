package me.moiclathy.dism.nl.e2e

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NaturalLanguageStateE2ERunnerTest {
    @Test
    fun successfulCompilationAndApplicationProducesCompleteTrace() = kotlinx.coroutines.test.runTest {
        val runner = NaturalLanguageStateE2ERunner<String, String>(
            compile = { text -> if (text.isBlank()) null else text },
            apply = { "APPLIED" },
            statusOf = { it },
        )

        val result = runner.run("实验服放在宿舍")
        assertTrue(result.compiled)
        assertTrue(result.applied)
        assertEquals("APPLIED", result.status)
        assertEquals(7, result.stageNames.size)
    }

    @Test
    fun compilationRejectionStopsBeforeStateApplication() = kotlinx.coroutines.test.runTest {
        var applied = false
        val runner = NaturalLanguageStateE2ERunner<String, String>(
            compile = { null },
            apply = { applied = true; "APPLIED" },
            statusOf = { it },
        )

        val result = runner.run("")
        assertFalse(result.compiled)
        assertFalse(result.applied)
        assertEquals("COMPILATION_REJECTED", result.status)
        assertFalse(applied)
    }
}
