package me.moiclathy.dism.platform

import me.moiclathy.dism.api.Dism
import me.moiclathy.dism.api.DismInternalApi
import me.moiclathy.dism.api.DismPublicApi
import me.moiclathy.dism.config.DismConfiguration
import me.moiclathy.dism.persistence.DismPersistence
import me.moiclathy.dism.persistence.string.JsonDynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.persistence.string.LocalStorageStringStorage

/**
 * 16.34：Web 的 DISM 装配。
 *
 * ```kotlin
 * val dism = WebDismFactory.create()
 * ```
 *
 * ### 与桌面 / Android 的关键区别
 *
 * 那两个平台能跑 SQLDelight，所以决策历史落在真正的数据库里。
 * 浏览器没有 SQLite driver，于是：
 *
 * ```text
 * 决策历史    localStorage（JSON）   ← 这里
 * 其它端口    内存                   ← 见下
 * ```
 *
 * ### 为什么只换了一个端口
 *
 * `DismPersistence` 有 12 个端口。只换 `scheduleSnapshots`
 * 不是偷懒，而是因为**其余 11 个在当前应用里从来没有被写入**：
 *
 * ```text
 * entities / evidence / relationStates / transitions
 *     ↑ 由语义层写入，而应用目前只做"导入日历 + 评估决策"
 * ```
 *
 * 也就是说它们在浏览器里始终是空的。为空的表实现持久化，
 * 换不来任何用户可见的效果，却要写 11 份无法验证的代码。
 *
 * 而 `scheduleSnapshots` 是**决策历史**——History 页读的就是它，
 * 也是"刷新页面就没了"的那一部分。
 *
 * ### 这个取舍的代价，要说清楚
 *
 * ```text
 * 刷新页面  日历还在（localStorage）
 *           决策历史还在（localStorage）   ← 16.34 新增
 *           语义状态没了（内存，但本来也没写过东西）
 * ```
 *
 * 等应用真的开始产生语义状态，这里就要补上对应的端口。
 * 那时应该重新评估 localStorage 是否还合适。
 */
@DismPublicApi
object WebDismFactory {

    @OptIn(DismInternalApi::class)
    fun create(
        configuration: DismConfiguration =
            DismConfiguration(),
    ): Dism =
        me.moiclathy.dism.bootstrap.DismBootstrap.create(
            persistence =
                DismPersistence(
                    scheduleSnapshots =
                        JsonDynamicScheduleDecisionSnapshotStore(
                            storage =
                                LocalStorageStringStorage(),
                        ),
                ),

            configuration = configuration,
        )
}
