package me.moiclathy.dism.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

import me.moiclathy.dism.config.DismConfiguration
import me.moiclathy.dism.config.DismStorageConfiguration
import me.moiclathy.dism.config.DismStorageMode
import me.moiclathy.dism.diagnostics.DismDiagnosticLevel
import me.moiclathy.dism.diagnostics.DismDiagnosticSinks
import me.moiclathy.dism.diagnostics.DismDiagnosticsConfiguration
import me.moiclathy.dism.diagnostics.InMemoryDismDiagnosticSink

/**
 * 15.5 / 15.7
 *
 * 诊断 sink 必须**真的接上**。
 *
 * ### 为什么这条测试重要
 *
 * `DismDiagnosticsConfiguration` 是一个很容易做成
 * "配置里有、实际没人用"的字段。那样的话：
 *
 * ```text
 * 应用接了一个 sink → 一条事件都收不到 → 完全不知道为什么
 * ```
 *
 * 所以这里断言的是**事件真的到达**，而不是字段存在。
 */
class DismDiagnosticsWiringTest {

    @Test
    fun configuredSinkReceivesStartupDiagnostics() {

        val sink: InMemoryDismDiagnosticSink =
            DismDiagnosticSinks.inMemory()

        DismFactory.create(
            DismConfiguration(
                diagnostics =
                    DismDiagnosticsConfiguration(
                        sink = sink,
                    ),
            ),
        )

        val events = sink.snapshot()

        assertTrue(
            events.isNotEmpty(),
            "a configured sink must actually receive events",
        )

        val codes = events.map { it.code }

        assertTrue(
            codes.contains("DISM.BOOTSTRAP.STARTED"),
            "codes=$codes",
        )

        assertTrue(
            codes.contains("DISM.BOOTSTRAP.READY"),
            "codes=$codes",
        )

        val ready =
            events.single {
                it.code == "DISM.BOOTSTRAP.READY"
            }

        assertEquals(
            DismApiVersion.STRING,
            ready.attributes["apiVersion"],
        )

        assertEquals("bootstrap", ready.component)

        assertEquals(DismDiagnosticLevel.INFO, ready.level)
    }

    @Test
    fun disabledDiagnosticsEmitNothing() {

        val sink: InMemoryDismDiagnosticSink =
            DismDiagnosticSinks.inMemory()

        DismFactory.create(
            DismConfiguration(
                diagnostics =
                    DismDiagnosticsConfiguration(
                        enabled = false,
                        sink = sink,
                    ),
            ),
        )

        assertTrue(
            sink.snapshot().isEmpty(),
            "enabled=false must actually silence the sink",
        )
    }

    @Test
    fun localFailureIsAlsoReportedToTheSink() {

        val sink: InMemoryDismDiagnosticSink =
            DismDiagnosticSinks.inMemory()

        assertFailsWith<
            me.moiclathy.dism.api.DismException,
            > {
            DismFactory.create(
                DismConfiguration(
                    storage =
                        DismStorageConfiguration(
                            mode = DismStorageMode.LOCAL,
                            databaseName = "dism",
                        ),

                    diagnostics =
                        DismDiagnosticsConfiguration(
                            sink = sink,
                        ),
                ),
            )
        }

        /*
         * 关键：即使异常被上层吞掉，
         * 接了 sink 的应用仍然能从日志里看到原因。
         */
        val failureEvent =
            sink.snapshot().single {
                it.code == "DISM.STORAGE.UNAVAILABLE"
            }

        assertEquals(DismDiagnosticLevel.ERROR, failureEvent.level)

        assertEquals(
            "LOCAL",
            failureEvent.attributes["storageMode"],
        )

        assertEquals(
            "dism",
            failureEvent.attributes["databaseName"],
        )
    }
}
