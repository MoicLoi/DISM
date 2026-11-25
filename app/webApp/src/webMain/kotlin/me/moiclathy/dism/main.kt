package me.moiclathy.dism

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

import kotlin.time.Clock

import me.moiclathy.dism.platform.WebDismFactory
import me.moiclathy.dism.calendar.StartupCalendar
import me.moiclathy.dism.calendar.WebCalendarPicker
import me.moiclathy.dism.calendar.WebCalendarStore
import me.moiclathy.dism.diagnostics.AppDiagnosticSink

/**
 * 16.14 / 16.24：Web 入口（js / wasmJs 共用）。
 *
 * ### 存储
 *
 * ```text
 * 决策历史    内存（浏览器没有文件系统，SQLDelight 落盘不可用）
 * 日历原文    localStorage
 * ```
 *
 * 注意这两者的差异：日历原文能跨刷新存活，
 * 而决策历史**不能**。所以 Web 上：
 *
 * ```text
 * 刷新页面  →  日历还在（localStorage）
 *             历史还在（localStorage，16.34）
 * ```
 *
 * 后一条是 16.34 加上的。在此之前历史全在内存里，
 * 刷新即清空。
 *
 * ### ⚠ 只有编译验证
 *
 * 没有在浏览器里跑过 js 与 wasmJs 两个目标。
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {

    /*
     * 16.25：装配集中在 DismContainer，三端用同一套。
     *
     * Web 端"只创建一次"由这里保证——`main` 每个页面加载只跑一次。
     * 刷新页面会重新装配，但两类数据都在 localStorage 里：
     *
     * ```text
     * 日历原文    WebCalendarStore
     * 决策历史    WebDismFactory → JsonSnapshotStore → localStorage
     * ```
     *
     * 16.34 之前这里用的是 `DismFactory.create()`（纯内存），
     * 于是刷新一次历史就没了。
     */
    val container =
        DismContainer(
            dism = WebDismFactory.create(),

            calendarStore = WebCalendarStore(),

            diagnostics = AppDiagnosticSink(),

            fallbackModel = { now -> SampleCalendar.model(now) },
        )

    ComposeViewport {
        WithFontResourcesLoaded {
            App(
                dism = container.dism,

                initialModel =
                    container.initialModel(Clock.System.now()),

                calendarPicker = WebCalendarPicker(),

                diagnostics = container.diagnostics,

                onCalendarImported = { icsText ->
                    /*
                     * 与桌面 / Android 同理：写失败会抛出来。
                     *
                     * localStorage 在隐私模式下会抛——
                     * 那时用户会看到"导入成功但没存下来"的提示，
                     * 这是正确的，因为那确实是事实。
                     */
                    container.calendarStore.save(icsText)
                },
            )
        }
    }
}
