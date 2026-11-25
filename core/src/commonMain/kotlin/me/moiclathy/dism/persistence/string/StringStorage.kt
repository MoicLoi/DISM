package me.moiclathy.dism.persistence.string

/**
 * 16.34：一个**字符串键值存储**。
 *
 * ### 为什么抽这一层
 *
 * 浏览器没有文件系统，SQLDelight 的 driver 在 Web 上不可用。
 * 可选的后端：
 *
 * ```text
 * localStorage   kotlinx-browser 已提供，同步、简单     ← 用它
 * IndexedDB      kotlinx-browser **不**提供，
 *                要手写整套 externals（IDBFactory /
 *                IDBDatabase / IDBObjectStore / 事件回调…）
 * ```
 *
 * 但直接调用 `localStorage` 会让存储层只能在浏览器里测。
 * 抽成这个接口之后：
 *
 * ```text
 * JSON 存储逻辑     core/commonMain   可在 JVM 上完整测试
 * localStorage 绑定  core/webMain      很薄，且不需要测
 * ```
 *
 * 这与"把 `CalendarTextStore` 抽出来让三端共用"是同一个手法：
 * **把平台无关的部分从平台相关的外壳里剥出来。**
 */
interface StringStorage {

    /**
     * @return 不存在时返回 null
     */
    fun get(key: String): String?

    fun set(key: String, value: String)

    fun remove(key: String)
}

/**
 * 内存实现。
 *
 * 两个用途：
 *
 * ```text
 * 测试     不必碰真实的 localStorage
 * 其它平台 桌面 / Android 若想用 JSON 存储而不是 SQLite
 *          （目前没这么做，但接口允许）
 * ```
 */
class InMemoryStringStorage(
    initial: Map<String, String> = emptyMap(),
) : StringStorage {

    private val values = LinkedHashMap(initial)

    override fun get(key: String): String? = values[key]

    override fun set(key: String, value: String) {
        values[key] = value
    }

    override fun remove(key: String) {
        values.remove(key)
    }

    /**
     * 当前全部键值。测试用。
     */
    fun snapshot(): Map<String, String> =
        values.toMap()
}
