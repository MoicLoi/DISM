package me.moiclathy.dism.nl

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class IntentCompilerTest {
    private val now = Instant.parse("2026-09-15T06:00:00Z")

    @Test
    fun createsEventFromReminderPhrase() {
        val result = DeterministicIntentCompiler.compile(
            NaturalLanguageInput("明天实验课，提醒我带实验服", observedAt = now)
        )
        assertEquals(SemanticIntentType.CREATE_EVENT, result.intents.single().type)
        assertTrue(result.intents.single().confidence >= 0.7)
    }

    @Test
    fun detectsRequirement() {
        val result = DeterministicIntentCompiler.compile(
            NaturalLanguageInput("实验课需要带实验服", observedAt = now)
        )
        assertEquals(SemanticIntentType.ADD_REQUIREMENT, result.intents.single().type)
        assertTrue(result.intents.single().mentions.any { it.surface.contains("实验服") })
    }

    @Test
    fun detectsLocationWithoutCreatingEntity() {
        val result = DeterministicIntentCompiler.compile(
            NaturalLanguageInput("实验服放在宿舍", observedAt = now)
        )
        assertEquals(SemanticIntentType.SET_LOCATION, result.intents.single().type)
        assertTrue(result.intents.single().mentions.isNotEmpty())
    }

    @Test
    fun unknownInputIsLowConfidenceAndWarns() {
        val result = DeterministicIntentCompiler.compile(
            NaturalLanguageInput("随便说点什么", observedAt = now)
        )
        assertEquals(SemanticIntentType.UNKNOWN, result.intents.single().type)
        assertTrue(result.intents.single().confidence < 0.5)
        assertTrue(result.warnings.isNotEmpty())
    }

    @Test
    fun emptyInputProducesNoIntent() {
        val result = DeterministicIntentCompiler.compile(
            NaturalLanguageInput("   ", observedAt = now)
        )
        assertTrue(result.intents.isEmpty())
    }
}
