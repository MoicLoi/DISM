package me.moiclathy.dism

import android.app.Application
import android.content.Context

import me.moiclathy.dism.calendar.AndroidCalendarStore
import me.moiclathy.dism.diagnostics.AppDiagnosticSink
import me.moiclathy.dism.diagnostics.DismDiagnosticsConfiguration
import me.moiclathy.dism.config.DismConfiguration
import me.moiclathy.dism.platform.AndroidDismFactory

/**
 * 16.25：Android 的应用级容器宿主。
 *
 * ### 它修掉的是什么
 *
 * 之前 `Dism` 挂在 `MainActivity` 的字段上：
 *
 * ```text
 * 旋转屏幕 / 切深色模式  →  Activity 重建
 *                        →  新的 Dism、新的数据库连接
 *                        →  旧的那份被丢给 GC，连接没关
 * ```
 *
 * Compose 与 Android 都可能为了配置变化重建 Activity，
 * 所以"把长生命周期对象挂在 Activity 上"是错的。
 * 正确的归属是 [Application]——它的生命周期覆盖整个进程。
 *
 * ### 为什么用 `by lazy` 而不是在 `onCreate` 里建
 *
 * ```text
 * onCreate 里建   →  每次冷启动都要打开数据库并跑迁移，
 *                    即使用户只是启动后又立刻退出
 * by lazy          →  第一次真正用到时才建
 * ```
 *
 * 代价是"首次进入界面"那一刻的延迟，而那是可接受的：
 * 数据库打开本来就必须发生一次。
 *
 * ### 它是线程安全的吗
 *
 * `by lazy` 默认是 `SYNCHRONIZED`，所以是。
 * 并发首次访问只会建一个实例。
 */
class DismApplication : Application() {

    /**
     * 整个进程共享一份。
     */
    val container: DismContainer by lazy {
        createContainer(this)
    }
}

/**
 * 装配容器。
 *
 * 抽成顶层函数而不是写在 [DismApplication] 里，
 * 是为了让"怎么装配"和"什么时候装配"分开——
 * 前者是策略，后者是生命周期。
 */
private fun createContainer(
    context: Context,
): DismContainer {

    val appContext = context.applicationContext

    /*
     * 16.23：诊断出口。
     *
     * Android 上没有控制台可看，所以真正的用途是
     * "留在内存里，供将来的诊断页/崩溃上报读取"。
     * 打印仍然开着——`logcat` 会收到。
     */
    val diagnostics = AppDiagnosticSink()

    val dism =
        AndroidDismFactory.create(
            context = appContext,
            databaseName = "dism",
            configuration =
                DismConfiguration(
                    diagnostics =
                        DismDiagnosticsConfiguration(
                            enabled = true,
                            sink = diagnostics,
                        ),
                ),
        )

    return DismContainer(
        dism = dism,

        calendarStore = AndroidCalendarStore(appContext),

        diagnostics = diagnostics,

        /*
         * 兜底：还没有导入过任何日历时用示例数据。
         *
         * 这是**应用**的策略，不是容器的。
         * 接上设备日历之后这一行会被替换掉。
         */
        fallbackModel = { now -> SampleCalendar.model(now) },
    )
}
