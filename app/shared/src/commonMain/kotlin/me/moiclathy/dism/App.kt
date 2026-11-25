package me.moiclathy.dism

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

import me.moiclathy.dism.api.Dism
import me.moiclathy.dism.calendar.CalendarImportFeedback
import me.moiclathy.dism.calendar.CalendarImporter
import me.moiclathy.dism.calendar.ImportOutcome
import me.moiclathy.dism.calendar.CalendarPicker
import me.moiclathy.dism.presentation.DismPresenter
import me.moiclathy.dism.presentation.DismUiState
import me.moiclathy.dism.presentation.navigation.DismDestination
import me.moiclathy.dism.presentation.state.SemanticStatePresenter
import me.moiclathy.dism.presentation.state.SemanticStateUiState
import me.moiclathy.dism.schedule.api.DismScheduleImport
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import me.moiclathy.dism.ui.DismApp
import me.moiclathy.dism.ui.ImportActions

/**
 * 16.21：共享 Compose 入口。
 *
 * ```kotlin
 * // desktopApp
 * App(
 *     dism = JvmDismFactory.create(dbFile),
 *     initialModel = model,
 *     calendarPicker = AwtCalendarPicker(window),
 *     onCalendarImported = { store.save(it) },
 * )
 * ```
 *
 * ### `Dism` 由平台入口注入
 *
 * [Dism] 的构造需要平台知识（数据库路径 / driver / Context），
 * 那属于平台层。共享层只接收一个装配好的实例：
 *
 * ```text
 * 平台入口 → JvmDismFactory / AndroidDismFactory → Dism → App
 * ```
 *
 * ### 日历也是注入的，而且必须如此
 *
 * [CalendarPicker] 同样由平台提供：桌面是 AWT 对话框、
 * Android 是 SAF、Web 是 `<input type="file">`。
 *
 * ```text
 * calendarPicker == null  →  不显示 Import 入口
 * ```
 *
 * 没接线的平台不显示"点了没反应"的按钮——那比没有按钮更糟。
 *
 * ### ICS 文本回传给平台，由平台缓存
 *
 * 导入成功后调 [onCalendarImported]。
 *
 * 共享层**不**自己去写文件：路径是平台知识，
 * 而且"导入的日历存在哪"是应用策略，不是 Core 的。
 */
@Composable
fun App(
    dism: Dism,
    initialModel: DynamicScheduleModel,
    now: Instant = Clock.System.now(),
    calendarPicker: CalendarPicker? = null,

    /**
     * 16.23：应用自己的诊断出口。
     *
     * 传 null 表示不报告。桌面入口会传一个
     * [me.moiclathy.dism.diagnostics.AppDiagnosticSink]。
     */
    diagnostics:
        me.moiclathy.dism.diagnostics.DismDiagnosticSink? =
        null,
    onCalendarImported: (String) -> Unit = {},
) {

    /*
     * 模型是**可变状态**：导入一份新日历会替换它。
     *
     * 之前它是不可变参数，因为那时数据只会来自示例。
     */
    var model by remember {
        mutableStateOf(initialModel)
    }

    var state by remember {
        mutableStateOf(DismUiState())
    }

    var selected by remember {
        mutableStateOf(DismDestination.TODAY)
    }

    var feedback by remember {
        mutableStateOf<CalendarImportFeedback?>(null)
    }

    /*
     * 16.31：库级状态**按需**加载。
     *
     * 它的代价是四张全表读，与库的大小成正比；
     * 而使用它的那一页是 desktopOnly 的（手机端打不开）。
     *
     * 所以它不跟着下面的 `load()` 一起跑，而是在用户
     * 真的选中那一页时读一次。
     *
     * `isLoaded` 保证只读一次：来回切页面不会反复读全表。
     */
    var semanticState by remember {
        mutableStateOf(SemanticStateUiState())
    }

    val scope = rememberCoroutineScope()

    /*
     * 键包含 `model`：导入新日历后自动重新评估。
     */
    LaunchedEffect(dism, model, now, diagnostics) {
        val presenter =
            DismPresenter(
                dism = dism,
                diagnostics = diagnostics,
            )

        state =
            withContext(Dispatchers.Default) {
                presenter.load(
                    model = model,
                    now = now,
                )
            }
    }

    /*
     * 16.31：选中库级页面时才读。
     *
     * 键是 `dism` 与 `selected`：
     *
     * ```text
     * dism      换实例（例如重新装配）要重新读
     * selected  用户切到那一页时触发
     * ```
     *
     * 刻意**不含** `model`：这一页显示的是库的状态，
     * 与日程模型无关，导入新日历不该让它重读全表。
     */
    LaunchedEffect(dism, selected) {
        if (
            selected == DismDestination.SEMANTIC_STATE &&
            !semanticState.isLoaded
        ) {
            semanticState =
                withContext(Dispatchers.Default) {
                    SemanticStatePresenter(dism).load()
                }
        }
    }

    DismApp(
        state = state,
        selected = selected,
        onSelect = { selected = it },
        semanticState = semanticState,
        importActions =
            ImportActions(
                feedback = feedback,
                onDismissFeedback = { feedback = null },
                onImport =
                    calendarPicker?.let { picker ->
                        {
                            scope.launch {
                                val outcome =
                                    CalendarImporter(
                                        picker = picker,
                                        onImported =
                                            onCalendarImported,
                                    ).pickAndImport(now = now)

                                when (outcome) {
                                    /*
                                     * 用户取消：不动界面。
                                     */
                                    ImportOutcome.Cancelled -> Unit

                                    is ImportOutcome.Imported -> {
                                        model = outcome.model
                                        feedback =
                                            outcome.feedback
                                    }

                                    is ImportOutcome.Rejected -> {
                                        /*
                                         * 不进模型——没有可用的东西。
                                         * 但必须给反馈，否则用户
                                         * 点完什么都不发生，像坏了。
                                         */
                                        feedback =
                                            outcome.feedback
                                    }
                                }
                            }
                        }
                    },
            ),
    )
}
