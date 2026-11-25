package me.moiclathy.dism.calendar

import java.io.File
import java.nio.file.Files

import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * 16.21：导入日历的缓存。
 *
 * 这个类看起来太简单、不值得测，但它承担的是
 * **"导入的东西下次还在吗"** 这个用户能直接感知的承诺。
 */
class ImportedCalendarStoreTest {

    private val dir: File =
        Files.createTempDirectory("dism-calendar-store")
            .toFile()

    private val file = File(dir, "schedule.ics")

    private val store = ImportedCalendarStore(file)

    @AfterTest
    fun cleanUp() {
        dir.deleteRecursively()
    }

    @Test
    fun missingFileLoadsAsNullInsteadOfThrowing() {

        /*
         * 首次运行时缓存文件当然不存在。
         * 这不该是一次异常，而应该是"还没有导入过"。
         */
        assertNull(store.load())
    }

    @Test
    fun savedTextSurvivesANewStoreInstance() {

        store.save("BEGIN:VCALENDAR\nEND:VCALENDAR")

        /*
         * 用**新的实例**读，模拟下次启动。
         * 用同一个实例读证明不了任何事（可能只是读了内存里的字段）。
         */
        val reopened = ImportedCalendarStore(file)

        assertEquals(
            "BEGIN:VCALENDAR\nEND:VCALENDAR",
            reopened.load(),
        )
    }

    @Test
    fun parentDirectoriesAreCreated() {

        val nested =
            File(dir, "a/b/c/schedule.ics")

        val nestedStore = ImportedCalendarStore(nested)

        assertTrue(
            !nested.parentFile.exists(),
            "precondition: parent must not exist",
        )

        nestedStore.save("BEGIN:VCALENDAR\nEND:VCALENDAR")

        assertTrue(nested.parentFile.exists())

        assertEquals(
            "BEGIN:VCALENDAR\nEND:VCALENDAR",
            nestedStore.load(),
        )
    }

    @Test
    fun blankContentIsTreatedAsNothingImported() {

        store.save("   \n  ")

        /*
         * 空白文件与"没有文件"对用户是同一件事。
         * 如果这里返回空白字符串，上层会去解析它、
         * 得到一个"解析失败"的提示——而用户其实什么都没导入过，
         * 不该看到错误。
         */
        assertNull(store.load())
    }

    @Test
    fun saveFailureIsNotSwallowed() {

        /*
         * 保存失败**必须**让调用方知道，这与 load 吞异常是不对称的，
         * 而且是刻意的：
         *
         * ```text
         * load 失败  →  界面显示"没导入过"，符合事实
         * save 失败  →  界面显示"导入成功"，重启后没了 ← 骗人
         * ```
         *
         * 用一个"父路径是个文件"的非法目标来制造失败。
         */
        val blocker = File(dir, "blocker")
        blocker.writeText("i am a file, not a directory")

        val bad =
            ImportedCalendarStore(
                File(blocker, "schedule.ics"),
            )

        assertFailsWith<Exception> {
            bad.save("BEGIN:VCALENDAR\nEND:VCALENDAR")
        }
    }

    @Test
    fun clearRemovesTheCache() {

        store.save("BEGIN:VCALENDAR\nEND:VCALENDAR")

        store.clear()

        assertNull(store.load())

        /*
         * 重复 clear 不该抛异常：用户可能连点两次"清除导入"。
         */
        store.clear()
    }
}
