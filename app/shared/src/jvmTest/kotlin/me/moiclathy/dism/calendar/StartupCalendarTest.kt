package me.moiclathy.dism.calendar

import java.io.File
import java.nio.file.Files

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

import me.moiclathy.dism.schedule.api.DismScheduleImport
import me.moiclathy.dism.schedule.model.DynamicScheduleId
import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleItemType
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import me.moiclathy.dism.schedule.model.DynamicScheduleSource
import me.moiclathy.dism.schedule.model.DynamicScheduleState
import me.moiclathy.dism.schedule.model.DynamicScheduleTemporal

/**
 * 16.21：启动路径。
 *
 * 重点在**回退分支**：它是唯一会造成不可逆数据损失的路径。
 */
class StartupCalendarTest {

    private val dir: File =
        Files.createTempDirectory("dism-startup-calendar")
            .toFile()

    private val file = File(dir, "calendar.ics")

    private val store = ImportedCalendarStore(file)

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    private val now = Instant.parse("2026-09-09T08:00:00Z")

    private val validIcs =
        """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//DISM//Test//EN
        BEGIN:VEVENT
        UID:lab-1
        SUMMARY:Imported lecture
        DTSTART:20260909T090000Z
        DTEND:20260909T094000Z
        END:VEVENT
        END:VCALENDAR
        """.trimIndent()

    private fun fallbackModel() =
        DynamicScheduleModel(
            id = DynamicScheduleId("fallback"),
            revision = 1L,
            items =
                listOf(
                    DynamicScheduleItem(
                        id = DynamicScheduleItemId("fallback-item"),
                        scheduleId =
                            DynamicScheduleId("fallback"),
                        title = "Fallback",
                        type = DynamicScheduleItemType.EVENT,
                        source = DynamicScheduleSource.SYSTEM,
                        temporal =
                            DynamicScheduleTemporal.At(now),
                        state = DynamicScheduleState.UPCOMING,
                    ),
                ),
        )

    private fun load() =
        StartupCalendar.initialModel(
            store = store,
            now = now,
            fallback = ::fallbackModel,
        )

    @Test
    fun withoutCacheFallsBack() {

        assertEquals(
            listOf("Fallback"),
            load().items.map { it.title },
        )
    }

    @Test
    fun withValidCacheUsesTheImportedCalendar() {

        store.save(validIcs)

        assertEquals(
            listOf("Imported lecture"),
            load().items.map { it.title },
        )
    }

    @Test
    fun unparsableCacheFallsBackWithoutDestroyingIt() {

        store.save("this is not a calendar")

        val model = load()

        assertEquals(
            listOf("Fallback"),
            model.items.map { it.title },
        )

        /*
         * 关键断言。
         *
         * 解析失败时顺手清掉缓存看起来"很干净"，
         * 但那会把一次解析规则变更变成用户的永久数据丢失。
         */
        assertNotNull(
            store.load(),
            "an unparsable cache must be kept, not deleted",
        )

        assertEquals(
            "this is not a calendar",
            store.load(),
        )
    }

    @Test
    fun aLaterValidSaveRecovers() {

        store.save("this is not a calendar")

        load()

        /*
         * 回退之后，用户重新导入一份有效日历必须能恢复。
         * 如果上一步把缓存删了，这里仍然会成功——
         * 所以这条测试本身不能替代上一条，它验证的是"可恢复"。
         */
        store.save(validIcs)

        assertEquals(
            listOf("Imported lecture"),
            load().items.map { it.title },
        )
    }

    @Test
    fun cacheIsReparsedRatherThanStoredAsAModel() {

        /*
         * 缓存里存的必须是**原始 ICS 文本**。
         *
         * 如果哪天有人改成"把解析后的模型序列化存起来"，
         * 解析规则升级后旧数据就会一直停留在旧规则上。
         * 这条断言用"存进去能原样读出来、且能被重新解析"来锁住这个设计。
         */
        store.save(validIcs)

        assertEquals(validIcs, store.load())

        val reparsed =
            DismScheduleImport.fromIcs(
                icsText = store.load()!!,
                now = now,
                sourceId = "user-import",
            )

        assertTrue(reparsed.success)
    }
}
