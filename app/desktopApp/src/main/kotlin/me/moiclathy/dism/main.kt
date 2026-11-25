package me.moiclathy.dism

import java.io.File

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

import kotlin.time.Clock
import kotlin.time.Instant

import me.moiclathy.dism.api.Dism
import me.moiclathy.dism.config.DismConfiguration
import me.moiclathy.dism.diagnostics.DismDiagnosticsConfiguration
import me.moiclathy.dism.diagnostics.AppDiagnosticSink
import me.moiclathy.dism.calendar.AwtCalendarPicker
import me.moiclathy.dism.calendar.ImportedCalendarStore
import me.moiclathy.dism.calendar.StartupCalendar
import me.moiclathy.dism.platform.JvmDismFactory
import me.moiclathy.dism.schedule.api.DismScheduleImport
import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/**
 * 16.14 / 16.21：桌面入口 —— 真实落盘 + 真实日历导入。
 *
 * ```text
 * ~/.dism/dism.db          决策历史（SQLDelight）
 * ~/.dism/calendar.ics     用户导入的日历原文
 * ```
 *
 * ### 启动时模型从哪来
 *
 * ```text
 * 1. 缓存的 calendar.ics（用户导入过）  → 重新解析
 * 2. SampleCalendar（第一次运行）
 * ```
 *
 * 注意是**重新解析**，而不是保存解析后的模型：
 *
 * ```text
 * 存原始输入 + 每次重新解析
 *   → 解析规则升级后自动得到新结果
 *
 * 存解析后的模型
 *   → 会一直停留在旧规则上，且与代码里的规则不一致
 * ```
 *
 * ### 为什么这里没有 `@OptIn(DismInternalApi::class)`
 *
 * 落盘走 `JvmDismFactory`，导入走 `DismScheduleImport`——
 * 两条都是真正的公共 API。
 * 这正是 16.14 与 16.21 要达到的效果。
 */
fun main() = application {
    val root = dismHomeDirectory()

    /*
     * 16.25：装配集中在 DismContainer。
     *
     * 桌面端的"只创建一次"由这里保证——`main` 只跑一次，
     * 与 Android 用 Application 是同一个目的：
     *
     * ```text
     * Android   Application 持有
     * 桌面       进程（main）持有
     * Web        页面（main）持有
     * ```
     */
    val container =
        createContainer(
            databaseFile = File(root, "dism.db"),
            calendarFile = File(root, "calendar.ics"),
        )

    Window(
        onCloseRequest = ::exitApplication,
        title = "DISM",
    ) {
        App(
            dism = container.dism,

            initialModel =
                container.initialModel(Clock.System.now()),

            calendarPicker = AwtCalendarPicker(window),

            diagnostics = container.diagnostics,

            onCalendarImported = { icsText ->
                /*
                 * 保存失败会抛出，不吞它：
                 * 吞掉会让用户看到"导入成功"，重启后发现数据没了。
                 */
                container.calendarStore.save(icsText)
            },
        )
    }
}

/**
 * 16.25：桌面端的装配。
 *
 * 与 Android 的 `createContainer` 逐行对应——
 * 差异只在"数据库放哪、日历缓存放哪"。
 */
private fun createContainer(
    databaseFile: File,
    calendarFile: File,
): DismContainer {

    /*
     * 16.23：诊断出口。
     *
     * 同时"打印到控制台"与"留在内存里"：
     * 前者让开发时立刻看到，后者供将来的诊断页读取。
     *
     * 同一个实例传给两边：
     *
     * ```text
     * JvmDismFactory   →  Core 内部的诊断也走这里
     * App              →  展示层的诊断（例如记录失败）
     * ```
     */
    val diagnostics = AppDiagnosticSink()

    val dism =
        JvmDismFactory.create(
            databaseFile = databaseFile,

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

        calendarStore = ImportedCalendarStore(calendarFile),

        diagnostics = diagnostics,

        fallbackModel = { now -> SampleCalendar.model(now) },
    )
}

/**
 * `~/.dism/`
 *
 * 用用户主目录而不是工作目录：从 IDE 运行与从命令行运行的
 * 当前目录不同，用工作目录会导致"换个启动方式就看不到数据了"，
 * 而这在用户眼里就是"数据丢了"。
 */
private fun dismHomeDirectory(): File =
    File(
        System.getProperty("user.home"),
        ".dism",
    )
