package me.moiclathy.dism.context.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.time.Instant

class ContextModelTest {
    private val now = Instant.parse("2026-01-01T10:00:00Z")

    @Test
    fun activeClaimRespectsExpiry() {
        val claim = ContextClaim(
            id = "c1",
            contextId = ContextId("situation"),
            mode = ContextMode.IN_CLASS,
            observedAt = now,
            expiresAt = Instant.parse("2026-01-01T11:00:00Z"),
        )
        assertTrue(claim.isActiveAt(now))
        assertFalse(claim.isActiveAt(Instant.parse("2026-01-01T11:00:00Z")))
    }

    @Test
    fun modelReturnsStateById() {
        val id = ContextId("situation")
        val state = ContextState(id, ContextMode.IN_TRANSIT, observedAt = now)
        val model = ContextModel(now, mapOf(id to state))
        assertEquals(ContextMode.IN_TRANSIT, model.state(id)?.mode)
    }
}
