package me.moiclathy.dism

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts

import kotlin.time.Clock

import me.moiclathy.dism.calendar.AndroidCalendarPicker

/**
 * 16.14 / 16.24 / 16.25：Android 入口。
 *
 * ```text
 * filesDir/dism.db              决策历史（SQLDelight）
 * filesDir/dism-calendar.ics    用户导入的日历原文
 * ```
 *
 * ### 长生命周期的东西都在 Application 里
 *
 * `Dism`、日历缓存、诊断出口都由 [DismApplication] 持有，
 * 所以 Activity 因旋转 / 深色模式切换而重建时：
 *
 * ```text
 * 数据库连接    复用（不再泄漏旧的）
 * 日历缓存      复用
 * 诊断记录      保留（内存 sink 不会被清空）
 * ```
 *
 * Activity 只持有**与界面同生命周期**的东西：
 * SAF 的 launcher 与 picker。
 *
 * ### SAF 的注册时机
 *
 * `registerForActivityResult` 必须在 Activity 进入 STARTED
 * **之前**注册，所以它作为字段初始化式出现。
 *
 * ### `calendarPicker` 为什么是 `lateinit`
 *
 * launcher 的回调需要 picker，picker 需要 `contentResolver`。
 * 两边互相引用，所以分两步：
 *
 * ```text
 * 字段初始化   launcher（回调里引用 calendarPicker）
 * onCreate    picker 建好后把 requestOpenDocument 接到 launcher
 * ```
 *
 * 回调真正触发时（用户点了 SAF）picker 必然已初始化。
 *
 * ### ⚠ 只有编译验证
 *
 * 本机没有 Android 环境。落盘、SAF、缓存、
 * 以及"旋转后连接确实被复用"这三条都**没有在真机上验证过**。
 */
class MainActivity : ComponentActivity() {

    private val container: DismContainer
        get() =
            (application as DismApplication).container

    private lateinit var calendarPicker: AndroidCalendarPicker

    private val openDocument =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument(),
        ) { uri ->
            calendarPicker.onDocumentPicked(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        calendarPicker =
            AndroidCalendarPicker(contentResolver).apply {
                requestOpenDocument = { types ->
                    openDocument.launch(types)
                }
            }

        super.onCreate(savedInstanceState)

        val container = container

        /*
         * `now` 每**次创建**都会重新读一次。
         *
         * 这是有意的：界面上的"今天"应该以进入界面那一刻为准。
         * 而模型本身来自持久化的日历原文，不会因为重建而丢失。
         */
        val now = Clock.System.now()

        setContent {
            App(
                dism = container.dism,

                initialModel = container.initialModel(now),

                calendarPicker = calendarPicker,

                diagnostics = container.diagnostics,

                onCalendarImported = { icsText ->
                    /*
                     * 与桌面 / Web 同理：保存失败会抛出来。
                     */
                    container.calendarStore.save(icsText)
                },
            )
        }
    }
}
