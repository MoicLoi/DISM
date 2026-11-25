package me.moiclathy.dism.calendar

import kotlin.time.Instant

import me.moiclathy.dism.schedule.api.DismScheduleImport
import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/**
 * 16.21：启动时该用什么模型。
 *
 * ```text
 * 有缓存且能解析    → 用缓存解析出来的
 * 有缓存但解析不了  → 退回 fallback，**不删缓存**
 * 没有缓存          → 退回 fallback
 * ```
 *
 * ### 为什么单独抽出来
 *
 * 它本来是平台入口里的一段私有代码，测不到。
 * 而这段代码里最容易静默出错的就是**第二条分支**：
 *
 * ```text
 * 如果解析失败时顺手删掉缓存
 *   → 用户会因为一次解析规则变更而永久丢失导入的日历
 * ```
 *
 * 那是一个不可逆的数据损失，必须有测试兜住。
 */
object StartupCalendar {

    fun initialModel(
        store: CalendarTextStore,

        now: Instant,

        sourceId: String = "user-import",

        /**
         * 没有可用缓存时的兜底。
         *
         * 用参数而不是直接引用示例数据，
         * 是为了让这个函数保持纯粹、可在测试里注入。
         */
        fallback: () -> DynamicScheduleModel,
    ): DynamicScheduleModel {

        val cached = store.load()

            ?: return fallback()

        val parsed =
            DismScheduleImport.fromIcs(
                icsText = cached,
                now = now,
                sourceId = sourceId,
            )

        return parsed.model

            /*
             * 解析失败。**刻意不删除缓存、也不清空它。**
             *
             * 解析规则变严、或文件被手工改坏，都只应该导致
             * "这次退回兜底"，而不是"用户的日历没了"。
             */
            ?: fallback()
    }
}
