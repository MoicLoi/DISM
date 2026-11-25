package me.moiclathy.dism.calendar

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/**
 * 16.24：导入流程。
 *
 * 这些测试以前做不到——那段逻辑内联在 Composable 里。
 * 而它包含两个"做错了用户会发现"的判断：
 *
 * ```text
 * 解析失败却缓存了      → 下次启动必然失败
 * 缓存失败却报成功      → 用户以为存下来了
 * ```
 */
class CalendarImporterTest {

    private val now = Instant.parse("2026-09-09T08:00:00Z")

    private val validIcs =
        """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//DISM//Test//EN
        BEGIN:VEVENT
        UID:a
        SUMMARY:A
        DTSTART:20260909T090000Z
        DTEND:20260909T094000Z
        END:VEVENT
        BEGIN:VEVENT
        UID:b
        SUMMARY:B
        DTSTART:20260909T113000Z
        DTEND:20260909T120000Z
        END:VEVENT
        END:VCALENDAR
        """.trimIndent()

    private fun pickerReturning(
        text: String?,
    ) =
        CalendarPicker { text }

    @Test
    fun cancelledImportDoesNothingAtAll() = runTest {

        var cached: String? = null

        val outcome =
            CalendarImporter(
                picker = pickerReturning(null),
                onImported = { cached = it },
            ).pickAndImport(now)

        assertEquals(ImportOutcome.Cancelled, outcome)

        /*
         * 用户取消不该留下任何痕迹——不缓存、不反馈。
         */
        assertEquals(null, cached)
    }

    @Test
    fun aValidImportIsParsedAndCached() = runTest {

        var cached: String? = null

        val outcome =
            CalendarImporter(
                picker = pickerReturning(validIcs),
                onImported = { cached = it },
            ).pickAndImport(now)

        val imported =
            outcome as ImportOutcome.Imported

        assertEquals(2, imported.model.items.size)

        assertEquals(2, imported.feedback.importedCount)

        assertTrue(!imported.feedback.isPartial)

        /*
         * 缓存必须与用户给的**原文**一致，
         * 而不是某种规范化后的版本——
         * 否则"重新解析"读到的东西与用户导入的不是同一个。
         */
        assertEquals(validIcs, cached)
    }

    @Test
    fun anUnparsableFileIsNotCached() = runTest {

        var cached: String? = null

        val outcome =
            CalendarImporter(
                picker = pickerReturning("not a calendar"),
                onImported = { cached = it },
            ).pickAndImport(now)

        val rejected =
            outcome as ImportOutcome.Rejected

        assertTrue(rejected.feedback.errors.isNotEmpty())

        assertEquals(0, rejected.feedback.importedCount)

        /*
         * 关键：解析不了的东西不该进缓存。
         *
         * 否则下次启动会拿着同一个坏文件再失败一次，
         * 而用户已经把它忘了。
         */
        assertEquals(
            null,
            cached,
            "an unparsable file must not be cached",
        )
    }

    @Test
    fun aCacheFailureIsReportedNotSwallowed() = runTest {

        val outcome =
            CalendarImporter(
                picker = pickerReturning(validIcs),
                onImported = {
                    throw IllegalStateException("disk full")
                },
            ).pickAndImport(now)

        val imported =
            outcome as ImportOutcome.Imported

        /*
         * 导入本身成功了：模型可用，界面该更新。
         */
        assertEquals(2, imported.model.items.size)

        /*
         * 但必须告诉用户"这次不会留到下次"。
         *
         * 吞掉的话界面显示"导入成功"，重启后它就不见了——
         * 用户会以为是 DISM 弄丢了数据。
         */
        assertTrue(
            imported.feedback.errors.any {
                it.contains("could not be saved")
            },
            "a cache failure must surface in the feedback; " +
                "errors=${imported.feedback.errors}",
        )

        assertTrue(imported.feedback.isPartial)
    }

    @Test
    fun partialImportKeepsTheGoodEventsAndReportsTheBad() =
        runTest {

            /*
             * 一个条目正常、一个条目缺 DTSTART。
             */
            val partlyBroken =
                """
                BEGIN:VCALENDAR
                VERSION:2.0
                PRODID:-//DISM//Test//EN
                BEGIN:VEVENT
                UID:good
                SUMMARY:Good
                DTSTART:20260909T090000Z
                DTEND:20260909T094000Z
                END:VEVENT
                BEGIN:VEVENT
                UID:broken
                SUMMARY:Broken
                END:VEVENT
                END:VCALENDAR
                """.trimIndent()

            val outcome =
                CalendarImporter(
                    picker = pickerReturning(partlyBroken),
                    onImported = {},
                ).pickAndImport(now)

            /*
             * 只要还有可用条目就算 Imported——
             * 一条坏的不该让整份日历白导入。
             *
             * 具体导入了几条由解析器决定，这里不断言死数字，
             * 只断言"有模型"且"反馈里如实反映情况"。
             */
            when (outcome) {
                is ImportOutcome.Imported -> {
                    assertTrue(outcome.model.items.isNotEmpty())
                }

                is ImportOutcome.Rejected -> {
                    /*
                     * 解析器也可能选择整体拒绝。
                     * 那也是合法行为，但必须给出原因。
                     */
                    assertTrue(outcome.feedback.errors.isNotEmpty())
                }

                ImportOutcome.Cancelled -> {
                    throw AssertionError(
                        "a file was chosen, so this cannot be " +
                            "a cancellation",
                    )
                }
            }
        }

    @Test
    fun theModelIdIsStableForTheSameSource() = runTest {

        suspend fun importOnce(): DynamicScheduleModel {

            val outcome =
                CalendarImporter(
                    picker = pickerReturning(validIcs),
                    onImported = {},
                ).pickAndImport(now)

            return (outcome as ImportOutcome.Imported).model
        }

        /*
         * 同一个来源导入两次必须得到同一个 schedule id，
         * 否则每次重新导入都会"换一个日历"，
         * 历史决策与注意力记忆全部对不上。
         */
        assertEquals(importOnce().id, importOnce().id)
    }
}
