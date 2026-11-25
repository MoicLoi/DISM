package me.moiclathy.dism.nl.query.e2e

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SemanticQueryE2ERunnerTest {
    @Test
    fun successfulQueryIsRecordedAsAnswered() {
        val runner = SemanticQueryE2ERunner<String, String, String, String>(
            compile = { if (it.isNotBlank()) it else null },
            resolve = { "resolved:$it" },
            executable = { true },
            execute = { "state:$it" },
            render = { "answer:$it" },
        )
        val trace = runner.run("实验服在哪里？")
        assertTrue(trace.compiled)
        assertTrue(trace.resolved)
        assertTrue(trace.executable)
        assertTrue(trace.answered)
        assertEquals("answer:state:resolved:实验服在哪里？", trace.response)
    }

    @Test
    fun unresolvedQueryNeverExecutes() {
        var executed = false
        val runner = SemanticQueryE2ERunner<String, String, String, String>(
            compile = { "candidate" },
            resolve = { null },
            executable = { true },
            execute = { executed = true; "state" },
            render = { "answer" },
        )
        val trace = runner.run("那个东西在哪里？")
        assertFalse(trace.resolved)
        assertFalse(trace.answered)
        assertFalse(executed)
    }
}
