package me.moiclathy.dism.persistence.string

import kotlinx.browser.localStorage

/**
 * 16.34：把 [StringStorage] 接到浏览器的 `localStorage`。
 *
 * ### 为什么不用 IndexedDB
 *
 * `kotlinx-browser` 提供 `window` / `document` / `localStorage` /
 * `sessionStorage`，但**不含 IndexedDB**。
 *
 * 用它就得手写整套 externals：
 *
 * ```text
 * IDBFactory / IDBDatabase / IDBObjectStore / IDBTransaction
 * IDBRequest / IDBOpenDBRequest / IDBVersionChangeEvent / 各种回调
 * ```
 *
 * 那是几百行无法验证的声明，而换来的只是更大的配额
 * （localStorage 约 5MB，IndexedDB 通常几十 MB 起）。
 *
 * 对**决策历史**这个量级——按"决策变化"追加，
 * 单条几百字节——5MB 足够。真要超了，
 * 应该分片键（`snapshot:<id>`）或换后端，
 * 而 [StringStorage] 这个接口允许那样演进。
 *
 * ### 这个类很薄，而且**不需要**被测试
 *
 * 它只做键值转发。所有有判断的逻辑（编解码、
 * 最新一条的取舍、query 的排序与截断、损坏数据的处理）
 * 都在 `JsonDynamicScheduleDecisionSnapshotStore` 里，
 * 而那个类在 JVM 上有 14 条测试。
 *
 * 这是把逻辑与绑定分开的直接收益：
 * **不可验证的部分被压到最小。**
 */
class LocalStorageStringStorage : StringStorage {

    override fun get(key: String): String? =
        try {
            localStorage.getItem(key)
        } catch (_: Exception) {
            /*
             * 隐私模式 / 存储被禁用时读取会抛。
             *
             * 退化成"没有历史"，而不是让应用起不来：
             * 历史丢了可恢复，应用打不开不可恢复。
             */
            null
        }

    override fun set(key: String, value: String) {
        /*
         * 写入**不吞**异常。
         *
         * 与桌面 / Android 的日历缓存同理：吞掉会让
         * "这次记录成功了"变成谎话。配额超限（QuotaExceededError）
         * 是真实且可行动的信息，调用方需要看到它。
         */
        localStorage.setItem(key, value)
    }

    override fun remove(key: String) {
        try {
            localStorage.removeItem(key)
        } catch (_: Exception) {
            // 删除失败没有可行动的后续动作，忽略。
        }
    }
}
