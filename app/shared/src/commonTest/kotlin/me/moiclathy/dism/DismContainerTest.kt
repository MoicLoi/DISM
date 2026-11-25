package me.moiclathy.dism

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Instant

import me.moiclathy.dism.api.DismFactory
import me.moiclathy.dism.calendar.CalendarTextStore
import me.moiclathy.dism.diagnostics.AppDiagnosticSink
import me.moiclathy.dism.schedule.api.DismScheduleImport
import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/**
 * 16.25：应用容器。
 *
 * 容器本身不引用 `Context` / `File`，所以能在这里直接测——
 * "只创建一次"由平台宿主保证，而"装配出来的东西彼此一致"
 * 由这些测试保证。
 */
class DismContainerTest {

    private val now = Instant.parse("2026-09-09T08:00:00Z")

    /**
     * 内存实现：让容器测试不必碰磁盘。
     *
     * 这也是当初把存储抽成 [CalendarTextStore] 接口的收益之一。
     */
    private class InMemoryStore(
        var value: String? = null,
    ) : CalendarTextStore {

        override fun load(): String? =
            value?.takeIf { it.isNotBlank() }

        override fun save(icsText: String) {
            value = icsText
        }
    }

    private val validIcs =
        """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//DISM//Test//EN
        BEGIN:VEVENT
        UID:imported
        SUMMARY:Imported
        DTSTART:20260909T090000Z
        DTEND:20260909T094000Z
        END:VEVENT
        END:VCALENDAR
        """.trimIndent()

    private fun container(
        store: CalendarTextStore = InMemoryStore(),
        fallback: (Instant) -> DynamicScheduleModel =
            { Instant ->
                throw AssertionError(
                    "fallback should not be used here",
                )
            },
    ) =
        DismContainer(
            dism = DismFactory.create(),
            calendarStore = store,
            diagnostics = AppDiagnosticSink(print = false),
            fallbackModel = fallback,
        )

    @Test
    fun exposesTheSameInstancesItWasGiven() {

        val store = InMemoryStore()

        val sink = AppDiagnosticSink(print = false)

        val dism = DismFactory.create()

        val c =
            DismContainer(
                dism = dism,
                calendarStore = store,
                diagnostics = sink,
                fallbackModel = {
                    throw AssertionError("unused")
                },
            )

        /*
         * 容器必须是"持有"而不是"重新创建"。
         *
         * 如果哪天有人把它改成内部 `by lazy { DismFactory... }`，
         * 平台层就无法控制它什么时候、被创建几次，
         * 而"每次 Activity 重建都新开一个数据库连接"
         * 正是这个容器要修掉的问题。
         */
        assertSame(dism, c.dism)

        assertSame(store, c.calendarStore)

        assertSame(sink, c.diagnostics)
    }

    @Test
    fun initialModelUsesTheStoredCalendar() {

        val c = container(store = InMemoryStore(validIcs))

        assertEquals(
            listOf("Imported"),
            c.initialModel(now).items.map { it.title },
        )
    }

    @Test
    fun initialModelFallsBackWhenNothingIsStored() {

        val c =
            container(
                store = InMemoryStore(null),
                fallback = { fallbackNow ->
                    assertEquals(
                        now,
                        fallbackNow,
                        "the fallback must receive the same " +
                            "instant the caller passed in",
                    )

                    modelNamed("Fallback")
                },
            )

        assertEquals(
            listOf("Fallback"),
            c.initialModel(now).items.map { it.title },
        )
    }

    @Test
    fun fallbackReceivesTheCallersInstant() {

        /*
         * 时钟纪律：`now` 由调用方提供，容器不自己读时钟。
         *
         * 如果容器内部调 `Clock.System.now()`，
         * 兜底数据的时间锚点就会与界面上其它地方不一致——
         * 而且是那种"偶尔错一天"的难查问题。
         */
        var seen: Instant? = null

        val c =
            container(
                store = InMemoryStore(null),
                fallback = { fallbackNow ->
                    seen = fallbackNow
                    modelNamed("Fallback")
                },
            )

        val other = Instant.parse("2030-01-02T03:04:05Z")

        c.initialModel(other)

        assertEquals(other, seen)
    }

    @Test
    fun anUnparsableCacheFallsBackButIsKept() {

        val store = InMemoryStore("not a calendar")

        val c =
            container(
                store = store,
                fallback = { modelNamed("Fallback") },
            )

        assertEquals(
            listOf("Fallback"),
            c.initialModel(now).items.map { it.title },
        )

        /*
         * 与 StartupCalendar 的既有保证一致：
         * 解析失败不删缓存。
         *
         * 这里再断言一次，是因为容器是新的一层，
         * 很容易有人在它内部加一句"读不出来就清掉"——
         * 那会把一次解析失败变成用户数据的永久丢失。
         */
        assertTrue(
            store.value != null,
            "an unparsable cache must be kept",
        )
    }

    /**
     * 用一个只含该标题的最小 ICS 生成模型。
     *
     * 走真实的导入链路，而不是手工构造 Core 的模型类型：
     * 测试夹具与生产路径用同一套解析，夹具就不会
     * 悄悄偏离真实数据的形状。
     */
    private fun modelNamed(
        title: String,
    ): DynamicScheduleModel {

        val ics =
            """
            BEGIN:VCALENDAR
            VERSION:2.0
            PRODID:-//DISM//Test//EN
            BEGIN:VEVENT
            UID:x
            SUMMARY:$title
            DTSTART:20260909T090000Z
            DTEND:20260909T094000Z
            END:VEVENT
            END:VCALENDAR
            """.trimIndent()

        return DismScheduleImport
            .fromIcs(
                icsText = ics,
                now = now,
                sourceId = "test",
            )
            .model
            ?: error("the test fixture must parse")
    }
}
