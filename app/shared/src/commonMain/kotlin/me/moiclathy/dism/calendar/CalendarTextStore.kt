package me.moiclathy.dism.calendar

/**
 * 缓存用户导入的日历原文。
 *
 * ### 为什么要有这个接口
 *
 * 桌面用 `java.io.File`，Android 用 `context.filesDir`——
 * 而 `File` 相关的实现放在 `jvmMain` 时，`androidMain`
 * **看不到它**（两个源集互不可见）。
 *
 * 于是要么在两个源集里各写一份，要么抽出这个接口。
 * 抽接口还有个额外好处：`StartupCalendar` 因此可以
 * 在测试里被塞进一个内存实现，不必真的碰磁盘。
 */
interface CalendarTextStore {

    /**
     * 读取缓存；不存在或读不出来时返回 null。
     *
     * 读失败不该抛：那会让"缓存丢了"变成"应用起不来"。
     */
    fun load(): String?

    /**
     * 写入缓存。
     *
     * 写失败**要抛**：吞掉会让用户看到"导入成功"，
     * 而重启后数据没了——那是欺骗。
     */
    fun save(icsText: String)
}
