package me.moiclathy.dism.provider

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

import me.moiclathy.dism.diagnostics.DismDiagnostics
import me.moiclathy.dism.diagnostics.DismDiagnosticSinks

/**
 * 15.8
 *
 * Provider registry 的行为契约。
 *
 * 重点不是"能注册"，而是两件容易坏掉的事：
 *
 * ```text
 * 注册顺序必须稳定        → all() 不能被哈希顺序左右
 * 诊断必须真的发出去      → 否则 DISM.PROVIDER.* 只是文档
 * ```
 */
class DismProviderRegistryBehaviorTest {

    private class TestProvider(
        override val id: String = "test",
        override val version: String = "1.0.0",
    ) : DismProvider {

        override val name = "Test Provider"

        override val capabilities =
            setOf(DismProviderCapability.READ)
    }

    @Test
    fun allPreservesRegistrationOrder() {

        val registry = DefaultDismProviderRegistry()

        listOf("zeta", "alpha", "mid").forEach {
            registry.register(TestProvider(id = it))
        }

        /*
         * 必须是注册顺序，不是字母序，也不是哈希序。
         */
        assertEquals(
            listOf("zeta", "alpha", "mid"),
            registry.all().map { it.id },
        )
    }

    @Test
    fun reRegistrationReplacesInPlace() {

        val registry = DefaultDismProviderRegistry()

        registry.register(TestProvider(id = "a", version = "1.0.0"))
        registry.register(TestProvider(id = "b", version = "1.0.0"))

        registry.register(TestProvider(id = "a", version = "2.0.0"))

        assertEquals(
            listOf("a", "b"),
            registry.all().map { it.id },
            "re-registration must not move the entry to the end",
        )

        assertEquals(
            "2.0.0",
            assertNotNull(registry.find("a")).version,
        )

        assertEquals(2, registry.all().size)
    }

    @Test
    fun unregisterRemovesProvider() {

        val registry = DefaultDismProviderRegistry()

        registry.register(TestProvider())

        assertNotNull(registry.find("test"))

        registry.unregister("test")

        assertNull(registry.find("test"))

        assertTrue(registry.all().isEmpty())
    }

    @Test
    fun blankProviderIdIsRejected() {

        val registry = DefaultDismProviderRegistry()

        kotlin.test.assertFailsWith<IllegalArgumentException> {
            registry.register(TestProvider(id = ""))
        }
    }

    @Test
    fun registerAndUnregisterEmitDiagnostics() {

        val sink = DismDiagnosticSinks.inMemory()

        val registry =
            DefaultDismProviderRegistry(
                diagnostics = DismDiagnostics(sink = sink),
            )

        registry.register(TestProvider(id = "quequeer", version = "1.2.0"))

        registry.unregister("quequeer")

        val events = sink.snapshot()

        assertEquals(
            listOf(
                "DISM.PROVIDER.REGISTERED",
                "DISM.PROVIDER.UNREGISTERED",
            ),
            events.map { it.code },
        )

        val registered = events.first()

        assertEquals("provider", registered.component)

        assertEquals(
            "quequeer",
            registered.attributes["providerId"],
        )

        /*
         * version 必须在诊断里，这正是 15.8.9 加它的理由。
         */
        assertEquals(
            "1.2.0",
            registered.attributes["providerVersion"],
        )
    }
}
