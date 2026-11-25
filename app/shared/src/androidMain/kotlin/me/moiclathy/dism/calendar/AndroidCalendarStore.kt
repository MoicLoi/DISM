package me.moiclathy.dism.calendar

import android.content.Context
import java.io.File

/**
 * 16.24：Android 上的日历缓存。
 *
 * 落在应用私有目录：
 *
 * ```text
 * context.filesDir/dism-calendar.ics
 * ```
 *
 * ### 为什么不是 `File` 版的复用
 *
 * `ImportedCalendarStore` 在 `jvmMain`，而 `androidMain`
 * 看不到 `jvmMain` 的类——两个源集互不可见。
 *
 * 所以要靠 [CalendarTextStore] 这个公共接口把两边接起来：
 * 共享的 `StartupCalendar` 只认识接口，不关心背后是
 * `user.home` 还是 `filesDir`。
 *
 * 用 `filesDir` 而不是外部存储：
 *
 * ```text
 * filesDir          应用私有，卸载即清，无需权限
 * 外部存储 / scoped 需要权限与额外处理，而这是内部缓存
 * ```
 */
class AndroidCalendarStore(
    context: Context,
    fileName: String = DEFAULT_FILE_NAME,
) : CalendarTextStore {

    private val file =
        File(context.applicationContext.filesDir, fileName)

    init {
        require(fileName.isNotBlank()) {
            "fileName must not be blank"
        }
    }

    override fun load(): String? =
        try {
            if (file.exists() && file.isFile) {
                file.readText().takeIf { it.isNotBlank() }
            } else {
                null
            }
        } catch (_: Exception) {
            /*
             * 与桌面版同理：读失败退化成"没导入过"，
             * 不该让缓存问题变成启动问题。
             */
            null
        }

    override fun save(icsText: String) {
        /*
         * 与桌面版同理：写失败向上抛。
         * 吞掉会让界面说"导入成功"，而重启后它不见了。
         */
        file.parentFile?.mkdirs()

        file.writeText(icsText)
    }

    private companion object {
        const val DEFAULT_FILE_NAME = "dism-calendar.ics"
    }
}
