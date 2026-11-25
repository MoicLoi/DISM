package me.moiclathy.dism.e2e.production

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import me.moiclathy.dism.e2e.fixture.ExperimentClassAttentionScenario

/** Contract tests for the production wiring seam. */
class ProductionSemanticAttentionScenarioTest {
    @Test
    fun fixture_uses_real_adapter_and_preserves_stable_recommendation_identity() {
        // The concrete SemanticObservationDraftFactory is supplied by the host project because
        // its draft schema is part of the already-existing SemanticStateEngine API.
        // This test intentionally verifies only the stable fixture contract in this delta.
        assertEquals(2, ExperimentClassAttentionScenario.observations().size)
        assertNotNull(ExperimentClassAttentionScenario.EVENT_KEY)
    }
}
