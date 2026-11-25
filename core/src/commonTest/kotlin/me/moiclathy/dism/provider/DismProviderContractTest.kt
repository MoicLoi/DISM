package me.moiclathy.dism.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DismProviderContractTest {

    private class TestProvider : DismProvider {

        override val id =
            "test"

        override val name =
            "Test Provider"

        override val version =
            "1.0.0"

        override val capabilities =
            setOf(
                DismProviderCapability.READ,
                DismProviderCapability.SYNC,
            )
    }

    @Test
    fun registryCanRegisterAndFindProvider() {

        val registry =
            DefaultDismProviderRegistry()

        val provider =
            TestProvider()

        registry.register(
            provider,
        )

        val found =
            registry.find(
                "test",
            )

        assertNotNull(
            found,
        )

        assertEquals(
            "1.0.0",
            found.version,
        )
    }

    @Test
    fun registryReturnsSnapshot() {

        val registry =
            DefaultDismProviderRegistry()

        registry.register(
            TestProvider(),
        )

        val providers =
            registry.all()

        assertEquals(
            1,
            providers.size,
        )

        assertEquals(
            "test",
            providers.single().id,
        )
    }

    @Test
    fun providerObservationPreservesExternalIdentity() {

        val observation =
            DismProviderObservation(
                providerId = "test",
                observedAt =
                    kotlin.time.Clock.System.now(),
                externalKey =
                    "external-001",
                payload =
                    mapOf(
                        "title" to "Example",
                    ),
            )

        assertEquals(
            "test",
            observation.providerId,
        )

        assertEquals(
            "external-001",
            observation.externalKey,
        )

        assertEquals(
            "Example",
            observation.payload["title"],
        )
    }
}
