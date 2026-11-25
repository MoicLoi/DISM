package me.moiclathy.dism.attention.context

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AttentionContextPolicyTest {
    private val policy = AttentionContextPolicy()

    @Test
    fun normalUsesDefaultBudget() {
        assertEquals(5, policy.effectiveBudget(AttentionContext()))
    }

    @Test
    fun inClassStronglyReducesAttentionBudget() {
        assertEquals(1, policy.effectiveBudget(AttentionContext(AttentionContextMode.IN_CLASS)))
    }

    @Test
    fun sleepingSuppressesNonCriticalAndUsesZeroBudget() {
        val context = AttentionContext(AttentionContextMode.SLEEPING)
        assertEquals(0, policy.effectiveBudget(context))
        assertFalse(policy.allowsNonCritical(context))
    }

    @Test
    fun emergencyCanUseHardLimit() {
        assertEquals(8, policy.effectiveBudget(AttentionContext(AttentionContextMode.EMERGENCY)))
    }

    @Test
    fun explicitContextLimitCanOnlyReduceBudget() {
        val context = AttentionContext(maxVisibleItems = 2)
        assertEquals(2, policy.effectiveBudget(context))
    }

    @Test
    fun multiplierIsClampedByHardLimit() {
        val context = AttentionContext(attentionMultiplier = 10.0)
        assertEquals(8, policy.effectiveBudget(context))
        assertTrue(policy.allowsNonCritical(context))
    }
}
