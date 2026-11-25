package me.moiclathy.dism.schedule.api

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * 16.21：ICS → `DynamicScheduleModel` 的公共入口。
 *
 * 用**真实的** ICS 文本测试，而不是构造内部对象：
 * 这一层的价值恰恰在于"用户给一个文件，我们给出模型"。
 */
class DismScheduleImportTest {

    private val now = Instant.parse("2026-09-09T08:00:00Z")

    private val singleEventIcs =
        """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//DISM//Test//EN
        BEGIN:VEVENT
        UID:lab-1
        SUMMARY:实验动物学
        DTSTART:20260909T090000Z
        DTEND:20260909T094000Z
        LOCATION:实验楼201
        END:VEVENT
        END:VCALENDAR
        """.trimIndent()

    private val twoEventIcs =
        """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//DISM//Test//EN
        BEGIN:VEVENT
        UID:lab-1
        SUMMARY:实验动物学
        DTSTART:20260909T090000Z
        DTEND:20260909T094000Z
        END:VEVENT
        BEGIN:VEVENT
        UID:lunch-1
        SUMMARY:午餐
        DTSTART:20260909T113000Z
        DTEND:20260909T120000Z
        END:VEVENT
        END:VCALENDAR
        """.trimIndent()

    @Test
    fun importsARealCalendarFile() {

        val result =
            DismScheduleImport.fromIcs(
                icsText = singleEventIcs,
                now = now,
            )

        assertTrue(
            result.success,
            "a well-formed calendar must import; " +
                "errors=${result.errors}",
        )

        val model = assertNotNull(result.model)

        assertEquals(1, model.items.size)

        val item = model.items.single()

        assertEquals("实验动物学", item.title)

        assertEquals(
            Instant.parse("2026-09-09T09:00:00Z"),
            item.temporal.startAt,
        )

        assertFalse(result.hasPartialFailures)
    }

    @Test
    fun importsEveryEventInTheFile() {

        val result =
            DismScheduleImport.fromIcs(
                icsText = twoEventIcs,
                now = now,
            )

        val model = assertNotNull(result.model)

        assertEquals(2, model.items.size)

        /*
         * 断言**声明顺序**，而不是 `.sorted()`。
         *
         * 我第一版写成 `map { title }.sorted()` 再比对声明顺序，
         * 结果挂在了"午餐/实验动物学"的码点序上——
         * 那既没在测排序，又掩盖了"导入是否保持文件顺序"这件事。
         * 保持源文件顺序本身是有意义的：用户在日历里的排列不该被重排。
         */
        assertEquals(
            listOf("实验动物学", "午餐"),
            model.items.map { it.title },
        )
    }

    @Test
    fun isDeterministicForTheSameInput() {

        /*
         * 同一个文件导入两次必须得到同一个模型。
         *
         * 这不是理所当然的：内部 mapper 会生成
         * `DynamicScheduleId` / `DynamicScheduleItemId`。
         * 如果它用随机 Uuid，两次导入就会得到两组不同的 id，
         * 于是"重新导入日历"会变成"把所有条目都换了一遍"——
         * 历史决策、注意力记忆、适配权重全部对不上。
         */
        val first =
            DismScheduleImport.fromIcs(singleEventIcs, now)

        val second =
            DismScheduleImport.fromIcs(singleEventIcs, now)

        assertEquals(first.model, second.model)

        assertEquals(
            first.model?.items?.map { it.id },
            second.model?.items?.map { it.id },
        )
    }

    @Test
    fun differentSourcesProduceDifferentScheduleIds() {

        val a =
            DismScheduleImport.fromIcs(
                icsText = singleEventIcs,
                now = now,
                sourceId = "calendar-a",
            )

        val b =
            DismScheduleImport.fromIcs(
                icsText = singleEventIcs,
                now = now,
                sourceId = "calendar-b",
            )

        /*
         * 两份不同来源的日历即使内容相同，也必须是两个 schedule，
         * 否则导入第二份会覆盖第一份。
         */
        assertTrue(
            a.model?.id != b.model?.id,
            "different source ids must not collapse into one " +
                "schedule",
        )
    }

    @Test
    fun garbageInputFailsWithoutThrowing() {

        /*
         * 用户选错文件是**常规情况**，不是异常情况。
         * 所以它返回结果，而不是抛异常——
         * 调用方不该为"文件不是日历"写 try/catch。
         */
        val result =
            DismScheduleImport.fromIcs(
                icsText = "this is not a calendar at all",
                now = now,
            )

        assertFalse(result.success)

        assertNull(result.model)

        assertTrue(
            result.errors.isNotEmpty(),
            "a failed import must explain itself",
        )

        assertFalse(result.hasPartialFailures)
    }

    @Test
    fun emptyCalendarSucceedsWithAnEmptyModel() {

        val result =
            DismScheduleImport.fromIcs(
                icsText =
                    """
                    BEGIN:VCALENDAR
                    VERSION:2.0
                    PRODID:-//DISM//Test//EN
                    END:VCALENDAR
                    """.trimIndent(),
                now = now,
            )

        /*
         * 我第一版断言这里是**失败**，理由是"别让用户以为日历是空的"。
         * 那个判断是错的：
         *
         * ```text
         * 格式正确、确实没有事件   →  解析成功，模型有 0 条
         * 格式不对（选错文件）     →  失败，给 errors
         * ```
         *
         * 把"空日历"报成错误会让用户去找一个不存在的格式问题。
         * 而"0 条"本身是调用方可以如实展示的信息。
         */
        assertTrue(result.success)

        val model = assertNotNull(result.model)

        assertTrue(model.items.isEmpty())

        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun blankSourceIdIsACallerError() {

        /*
         * 与"用户数据错误"相反，空白 sourceId 是**调用方写错了**，
         * 由 domain 的 require 抛出。
         *
         * 它不该被降级成"导入失败"——那会让一个编程错误
         * 看起来像用户数据问题，从而永远查不出来。
         */
        var threw = false

        try {
            DismScheduleImport.fromIcs(
                icsText = singleEventIcs,
                now = now,
                sourceId = "   ",
            )
        } catch (error: IllegalArgumentException) {
            threw = true
        }

        assertTrue(
            threw,
            "a blank source id is a programming error, " +
                "not a user data error",
        )
    }
}
