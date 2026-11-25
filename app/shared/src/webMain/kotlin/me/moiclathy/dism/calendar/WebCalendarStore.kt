package me.moiclathy.dism.calendar

import kotlinx.browser.localStorage

/**
 * 16.24：浏览器上的日历缓存。
 *
 * ### 为什么是 `localStorage`
 *
 * 浏览器没有文件系统。可选项：
 *
 * ```text
 * localStorage       同步、简单、按 origin 隔离、约 5MB     ← 选它
 * IndexedDB          异步、能存大文件，接口复杂得多
 * 不缓存             每次刷新都要重新导入，体验差
 * ```
 *
 * ICS 通常几十 KB，5MB 绰绰有余。
 *
 * ### 它与桌面的一个真实差异
 *
 * ```text
 * 桌面 / Android   缓存的是"某个用户的文件"
 * 浏览器           localStorage 按 **origin** 隔离，
 *                 同一台机器上换个端口就是另一个存储
 * ```
 *
 * 所以开发时（端口常变）会感觉"数据丢了"。
 * 这与 `~/.dism/` 用主目录而不是工作目录是同一类问题的两面：
 * 存储位置会随启动方式变化。
 *
 * ### ⚠ 只有编译验证
 *
 * 没有在浏览器里跑过。
 */
class WebCalendarStore(
    private val key: String = DEFAULT_KEY,
) : CalendarTextStore {

    override fun load(): String? =
        try {
            localStorage.getItem(key)?.takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            /*
             * 隐私模式 / 禁用存储时读取会抛。
             *
             * 与桌面版同理：退化成"没导入过"，
             * 不该让存储问题变成应用起不来。
             */
            null
        }

    override fun save(icsText: String) {
        /*
         * 写失败**不吞**：与桌面 / Android 版保持一致。
         * 吞掉会让界面说"导入成功"，而刷新后它不见了。
         */
        localStorage.setItem(key, icsText)
    }

    private companion object {
        const val DEFAULT_KEY = "dism.calendar.ics"
    }
}
