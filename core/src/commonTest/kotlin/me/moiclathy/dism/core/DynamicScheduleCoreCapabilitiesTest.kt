package me.moiclathy.dism.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DynamicScheduleCoreCapabilitiesTest {

    @Test
    fun currentCapabilitiesExposeCompletedDecisionPipeline() {

        val capabilities =
            DynamicScheduleCoreCapabilities.current()

        assertTrue(
            capabilities.dynamicState,
        )

        assertTrue(
            capabilities.dynamicSemantics,
        )

        assertTrue(
            capabilities.dynamicWeight,
        )

        assertTrue(
            capabilities.attentionMemory,
        )

        assertTrue(
            capabilities.priorityAdaptation,
        )

        assertTrue(
            capabilities.conflictResolution,
        )

        assertTrue(
            capabilities.explanation,
        )

        assertTrue(
            capabilities.decisionTrace,
        )

        assertTrue(
            capabilities.decisionSnapshot,
        )

        assertTrue(
            capabilities.historicalReplay,
        )

        assertTrue(
            capabilities.replayVerification,
        )
    }

    @Test
    fun coreMetadataHasStableIdentity() {

        val metadata =
            DynamicScheduleCoreMetadata()

        assertEquals(
            "DISM Core",
            metadata.name,
        )

        assertTrue(
            metadata.version.isNotBlank(),
        )
    }
}
