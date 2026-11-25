package me.moiclathy.dism

import kotlin.time.Instant

import me.moiclathy.dism.api.Dism
import me.moiclathy.dism.calendar.CalendarTextStore
import me.moiclathy.dism.calendar.StartupCalendar
import me.moiclathy.dism.diagnostics.AppDiagnosticSink
import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/**
 * 16.25：一次装配好的应用容器。
 *
 * ```text
 * Dism              运行时（含数据库连接）
 * CalendarTextStore 日历缓存
 * AppDiagnosticSink 诊断出口
 * fallbackModel     没有任何导入时的兜底
 * ```
 *
 * ### 它解决的是什么
 *
 * 在它之前，各平台入口**各自**重复了同一段装配：
 *
 * ```kotlin
 * val dism = ...Factory.create(...)
 * val store = ...CalendarStore(...)
 * val model = StartupCalendar.initialModel(store, now) { SampleCalendar.model() }
 * ```
 *
 * 三段代码在三端各写一遍，于是任何一处改动都要记得改三次。
 *
 * ### 更重要的：生命周期
 *
 * 它同时是"**这个容器该被谁持有**"的答案：
 *
 * ```text
 * Android    Application（不是 Activity）
 * 桌面       进程（main 里一次）
 * Web        页面（main 里一次）
 * ```
 *
 * 容器本身不关心谁持有它——**这是它可测的原因**：
 * 它不引用 `Context`，也不引用 `File`。
 * 平台各自负责"只创建一次"，而这件事由平台的宿主保证：
 *
 * ```kotlin
 * // Android
 * class DismApplication : Application() {
 *     val container by lazy { DismContainer(...) }
 * }
 * ```
 *
 * ### 为什么 fallback 是参数
 *
 * `SampleCalendar` 是示例数据。生产容器不该把"用假的顶上"
 * 硬编码进去——万一将来接上真正的设备日历，
 * 兜底策略会变，而容器不该跟着改。
 */
class DismContainer(
    val dism: Dism,

    val calendarStore: CalendarTextStore,

    val diagnostics: AppDiagnosticSink,

    /**
     * 没有任何已导入日历时用什么。
     */
    private val fallbackModel: (Instant) -> DynamicScheduleModel,
) {

    /**
     * 启动时的模型。
     *
     * 每次调用都会**重新**读缓存并重新解析——
     * 见 `StartupCalendar` 的说明：存原始输入、每次重新解析，
     * 这样解析规则升级后旧数据自动得到新结果。
     *
     * 调用方（平台宿主）负责只调一次。
     */
    fun initialModel(
        now: Instant,
    ): DynamicScheduleModel =
        StartupCalendar.initialModel(
            store = calendarStore,
            now = now,
            fallback = { fallbackModel(now) },
        )
}
