package me.moiclathy.dism.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DismCoreContractTest {

    @Test
    fun apiVersionIsStable() {

        assertEquals(
            "1.5.0",
            DismApiVersion.STRING,
        )
    }

    @Test
    fun factoryCreatesDism() {

        val dism =
            DismFactory.create()

        assertNotNull(
            dism,
        )
    }

    @Test
    fun publicRuntimeIsAvailable() {

        val dism =
            DismFactory.create()

        assertNotNull(
            dism.runtime,
        )
    }

    @Test
    fun requiredCapabilitiesAreAdvertised() {

        val dism =
            DismFactory.create()

        assertTrue(
            dism.capabilities.semanticState,
        )

        assertTrue(
            dism.capabilities.dynamicSchedule,
        )

        assertTrue(
            dism.capabilities.attention,
        )

        assertTrue(
            dism.capabilities.persistence,
        )

        assertTrue(
            dism.capabilities.replay,
        )

        assertTrue(
            dism.capabilities.verification,
        )

        assertTrue(
            dism.capabilities.adaptation,
        )

        assertTrue(
            dism.capabilities.localFirst,
        )
    }
}
