package me.moiclathy.dism.presentation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

import kotlinx.coroutines.test.runTest
import kotlinx.datetime.TimeZone

import me.moiclathy.dism.api.DismFactory
import me.moiclathy.dism.diagnostics.AppDiagnosticSink
import me.moiclathy.dism.diagnostics.DECISION_RECORD_FAILED_CODE
import me.moiclathy.dism.diagnostics.DismDiagnosticLevel
import me.moiclathy.dism.schedule.model.DynamicScheduleId
import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleItemType
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import me.moiclathy.dism.schedule.model.DynamicScheduleSource
import me.moiclathy.dism.schedule.model.DynamicScheduleState
import me.moiclathy.dism.schedule.model.DynamicScheduleTemporal

/**
 * 16.23：记录失败必须**留下痕迹**。
 *
 * ### 为什么这条测试值得存在
 *
 * 16.22 有意吞掉了记录失败的异常，理由是
 * "宁可 History 少一条，也不能让 Today 变成错误页"。
 *
 * 那个理由是成立的，但吞掉之后如果不报告任何东西，
 * 就退化成**静默失败**：
 *
 * ```text
 * 用户看到      History 一直是空的
 * 开发者看到    什么都没有
 * ⇒ 没有人能查出来
 * ```
 *
 * 所以这里断言的是"吞掉，但报告"。
 */
class DecisionRecordingDiagnosticsTest {

    private val now = Instant.parse("2026-09-09T08:00:00Z")

    private val scheduleId = DynamicScheduleId("diag")

    private fun model() =
        DynamicScheduleModel(
            id = scheduleId,
            revision = 1L,
            items =
                listOf(
                    DynamicScheduleItem(
                        id = DynamicScheduleItemId("a"),
                        scheduleId = scheduleId,
                        title = "A",
                        type = DynamicScheduleItemType.EVENT,
                        source = DynamicScheduleSource.USER_INPUT,
                        temporal =
                            DynamicScheduleTemporal.At(
                                Instant.parse(
                                    "2026-09-09T09:00:00Z",
                                ),
                            ),
                        state = DynamicScheduleState.UPCOMING,
                    ),
                ),
        )

    private fun failingPresenter(
        sink: AppDiagnosticSink,
        error: Exception =
            IllegalStateException("disk full"),
    ) =
        DismPresenter(
            dism = DismFactory.create(),
            timeZone = TimeZone.UTC,
            diagnostics = sink,
            recordDecision = { _, _ -> throw error },
        )

    @Test
    fun aFailedRecordingEmitsADiagnosticEvent() = runTest {

        val sink = AppDiagnosticSink(print = false)

        failingPresenter(sink)
            .load(model = model(), now = now)

        val event =
            sink.events().singleOrNull {
                it.code == DECISION_RECORD_FAILED_CODE
            }

        assertTrue(
            event != null,
            "a swallowed failure must still be reported; " +
                "events=${sink.events()}",
        )

        assertEquals(DismDiagnosticLevel.WARN, event.level)

        assertEquals("presentation", event.component)

        assertEquals("recordDecision", event.operation)

        assertTrue(
            event.message.contains("disk full"),
            "the event must carry the original reason; " +
                "was='${event.message}'",
        )

        assertEquals(
            "IllegalStateException",
            event.attributes["errorType"],
        )
    }

    @Test
    fun aFailedRecordingDoesNotBreakTheScreen() = runTest {

        val sink = AppDiagnosticSink(print = false)

        val state =
            failingPresenter(sink)
                .load(model = model(), now = now)

        /*
         * 这是"有意降级"的那一半：页面必须照常可用。
         *
         * 如果这条失败，说明记录失败把 Today 页带崩了——
         * 那比"历史少一条"严重得多。
         */
        assertNull(state.errorMessage)

        assertEquals(
            listOf("a"),
            state.today.timeline.map { it.itemId },
        )
    }

    @Test
    fun aSuccessfulRecordingEmitsNothing() = runTest {

        val sink = AppDiagnosticSink(print = false)

        /*
         * 不注入失败 lambda，走真实的记录路径。
         */
        val presenter =
            DismPresenter(
                dism = DismFactory.create(),
                timeZone = TimeZone.UTC,
                diagnostics = sink,
            )

        presenter.load(model = model(), now = now)

        /*
         * 正常路径不该产生 WARN。
         *
         * 如果这条失败，说明诊断被用成了日志——
         * 那样"有没有失败"就淹没在噪声里了。
         */
        assertTrue(
            sink.events().none {
                it.level == DismDiagnosticLevel.WARN
            },
            "a successful run must not warn; " +
                "events=${sink.events()}",
        )
    }

    @Test
    fun withoutASinkTheFailureIsStillHarmless() = runTest {

        /*
         * diagnostics = null 的宿主（例如只做渲染的测试）
         * 不该因为"没有诊断出口"而炸掉。
         */
        val presenter =
            DismPresenter(
                dism = DismFactory.create(),
                timeZone = TimeZone.UTC,
                diagnostics = null,
                recordDecision = { _, _ ->
                    throw IllegalStateException("boom")
                },
            )

        val state = presenter.load(model = model(), now = now)

        assertNull(state.errorMessage)

        assertEquals(1, state.today.timeline.size)
    }

    @Test
    fun theFailureIsAlsoVisibleToTheUserNotJustDiagnostics() =
        runTest {

            val sink = AppDiagnosticSink(print = false)

            val state =
                failingPresenter(sink)
                    .load(model = model(), now = now)

            /*
             * 16.26：这条是关键。
             *
             * 16.23 把失败发进了诊断，但诊断是**开发者**的通道。
             * 只到那一步的话，用户在 History 页看到的仍然是：
             *
             * ```text
             * 一个空列表，没有任何解释
             * ```
             *
             * 而"页面看起来正常、内容是错的"是最难发现的一类问题。
             */
            assertTrue(
                state.history.historyMayBeIncomplete,
                "the UI must be able to tell the user that " +
                    "history may be incomplete",
            )

            assertTrue(
                state.history.recordingError
                    ?.contains("disk full") == true,
                "the reason must reach the UI verbatim; " +
                    "was='${state.history.recordingError}'",
            )
        }

    @Test
    fun aSuccessfulRecordingDoesNotMarkHistoryAsIncomplete() =
        runTest {

            val sink = AppDiagnosticSink(print = false)

            val state =
                DismPresenter(
                    dism = DismFactory.create(),
                    timeZone = TimeZone.UTC,
                    diagnostics = sink,
                ).load(model = model(), now = now)

            assertTrue(state.history.canRecord)

            assertNull(state.history.recordingError)

            assertTrue(!state.history.historyMayBeIncomplete)
        }

    @Test
    fun recordingRecoversOnceTheFailureGoesAway() = runTest {

        val sink = AppDiagnosticSink(print = false)

        /*
         * 第一次记录失败。
         */
        val failing =
            DismPresenter(
                dism = DismFactory.create(),
                timeZone = TimeZone.UTC,
                diagnostics = sink,
                recordDecision = { _, _ ->
                    throw IllegalStateException("disk full")
                },
            ).load(model = model(), now = now)

        assertTrue(failing.history.historyMayBeIncomplete)

        /*
         * 同一个 Dism、同一个模型，但这次记录成功。
         *
         * 提示必须**自动消失**：它反映的是"现在记录功能是否可用"，
         * 不是一个历史累计错误。如果它粘住不走，
         * 用户会以为问题一直在，而实际上早就好了。
         */
        val recovered =
            DismPresenter(
                dism = DismFactory.create(),
                timeZone = TimeZone.UTC,
                diagnostics = sink,
            ).load(model = model(), now = now)

        assertTrue(
            recovered.history.canRecord,
            "the warning must clear once recording works " +
                "again",
        )
    }

    @Test
    fun theReasonIsShownVerbatimRatherThanGeneric() = runTest {

        val sink = AppDiagnosticSink(print = false)

        val state =
            failingPresenter(
                sink = sink,
                error =
                    IllegalStateException(
                        "[SQLITE_READONLY] Attempt to write " +
                            "a readonly database",
                    ),
            ).load(model = model(), now = now)

        /*
         * 不要把原因吞成"未知错误"。
         *
         * 用户至少能据此判断"是不是磁盘满了 / 权限问题"，
         * 而不是只能报一个无法复现的 bug。
         */
        assertTrue(
            state.history.recordingError
                ?.contains("SQLITE_READONLY") == true,
        )
    }

    @Test
    fun theEventCodeIsStable() {

        /*
         * 断言用**代码**而不是 message。
         * 这句话是给人看的、随时会改；代码是契约。
         */
        assertEquals(
            "DECISION_RECORD_FAILED",
            DECISION_RECORD_FAILED_CODE,
        )
    }
}
