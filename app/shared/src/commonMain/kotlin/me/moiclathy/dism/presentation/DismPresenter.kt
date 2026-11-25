package me.moiclathy.dism.presentation

import kotlin.time.Instant

import me.moiclathy.dism.presentation.attention.AttentionUiState
import me.moiclathy.dism.presentation.history.HistoryUiState
import me.moiclathy.dism.presentation.home.HomeUiState
import me.moiclathy.dism.presentation.schedule.ScheduleUiState
import me.moiclathy.dism.presentation.settings.SettingsUiState

/**
 * 16.3：共享 UI 一次渲染所需的**日程相关**状态。
 *
 * ```text
 * DismUiState
 *  ├── today          Today 页
 *  ├── schedule       Schedule 页
 *  ├── attention      Attention 页
 *  ├── history        History 页（桌面 / Web）
 *  └── settings       Settings 页
 * ```
 *
 * ### 16.31：库级状态**不在这里**
 *
 * `Semantic State` 页需要 `runtime.snapshot()`，而那是
 * **四张全表读**——代价与库的大小成正比。它曾经每次 `load()`
 * 都跑一遍，而那一页是 `desktopOnly` 的（手机端打不开）。
 *
 * 现在它由 `SemanticStatePresenter` 在用户真的选中那一页时加载。
 *
 * 这条边界的意义是：**代价应该跟着"用户要看什么"走，
 * 而不是跟着"库有多大"走**。
 *
 * ### 为什么三个日程页面共用一次评估
 *
 * `facade.evaluate()` 要跑完整的
 * Dependency → Context → Attention Memory → Adaptation →
 * Attention Projection → Conflict → Explanation 链路。
 *
 * 如果每个页面各评估一次：
 *
 * ```text
 * 1. 同一个模型被算三遍（切换 tab 尤其明显）
 * 2. 三次之间若有状态变化，界面会出现互相矛盾的视图
 * ```
 *
 * 所以评估一次，三个页面从这里各自投影。
 *
 * ### 导航态不在这里
 *
 * "当前在哪个 tab"是 UI 自己的状态，不是 Core 的数据。
 * 把它塞进来会让 Presenter 需要知道导航结构。
 */
data class DismUiState(
    val today: HomeUiState = HomeUiState(dateLabel = ""),

    val schedule: ScheduleUiState = ScheduleUiState(),

    val attention: AttentionUiState = AttentionUiState(),


    val history: HistoryUiState = HistoryUiState(),

    val settings: SettingsUiState = SettingsUiState(),

    val isLoading: Boolean = true,
) {

    /**
     * 任意一个页面出了错，就认为这次加载失败。
     *
     * 注意**不含**库级状态：它由 `SemanticStatePresenter`
     * 在用户打开那一页时单独加载（16.31），
     * 所以它的错误不属于这次加载。
     */
    val errorMessage: String?
        get() =
            today.errorMessage
                ?: schedule.errorMessage
                ?: attention.errorMessage
                ?: history.errorMessage
                ?: settings.errorMessage
}

/**
 * 16.3：共享 UI 的**唯一**数据入口。
 *
 * ```text
 * Dism（Public API）
 *       ↓  evaluate 一次
 * DismPresenter
 *       ↓  三个纯投影
 * DismUiState → Compose
 * ```
 *
 * ### 时钟与时区都由调用方提供
 *
 * 与 13.15 的纪律一致：`now` 不在内部读取。
 * 时区同理——否则 UiState 会随运行机器变化。
 *
 * ### `preview` 而不是 `evaluate`：这是一个被修掉的真实缺口
 *
 * 这一层一开始调的是 `facade.evaluate(...)`，而它会**持久化**，
 * 于是出现：
 *
 * ```text
 * 打开 App 1 次    → 每个 item 多一条历史
 * 打开 App 100 次  → 每个 item 100 条重复记录
 * ```
 *
 * 这是 15.12 那句"让真实消费者把 API 的问题暴露出来"的真实例子——
 * UI 是第一个真正的消费者，一上手就撞到了。
 *
 * 依此给 `DynamicScheduleFacade` 增加了只读入口 `preview(model, now)`
 * （additive，按 SemVer 属 MINOR，公共 API 版本随之到 1.1.0），
 * 这一层现在只读、无副作用、可重复加载。
 *
 * ```text
 * preview  展示用：算一次，不写历史
 * evaluate 记录用：算一次并留下历史
 * ```
 */
class DismPresenter(
    private val dism: me.moiclathy.dism.api.Dism,

    private val timeZone: kotlinx.datetime.TimeZone =
        kotlinx.datetime.TimeZone.currentSystemDefault(),

    /**
     * 16.23：记录失败要报到哪里。
     *
     * 传 null 表示不报告（测试或不需要诊断的宿主）。
     */
    private val diagnostics:
        me.moiclathy.dism.diagnostics.DismDiagnosticSink? = null,

    /**
     * 16.23：记录策略，**可注入**。
     *
     * ### 为什么要开这个口子
     *
     * 失败路径需要一个"会失败的 `evaluate`"才能测。
     * 而构造一个 `Dism` 的假实现要连 `DismRuntimeApi` 一起实现，
     * 代价很大；制造真实的写失败（只读磁盘 / 磁盘满）
     * 在单元测试里也不可靠。
     *
     * 所以把这一步做成参数：测试传一个会抛异常的 lambda，
     * 就能确定性地验证"失败被吞掉、但事件被发出去了"。
     *
     * 生产路径用默认值，行为不变。
     */
    private val recordDecision:
        suspend (
            me.moiclathy.dism.schedule.model.DynamicScheduleModel,
            Instant,
        ) -> Boolean =
        { model, now ->
            DecisionRecorder(dism)
                .recordIfChanged(model = model, now = now)
        },
) {

    suspend fun load(
        model: me.moiclathy.dism.schedule.model.DynamicScheduleModel,
        now: Instant,
    ): DismUiState {

        /*
         * 错误只在一处捕获：UI 不该把内部异常泄到界面上，
         * 也不该因为一次评估失败就整页崩溃。
         *
         * `preview` 是只读的：算一次决策但不写历史。
         */
        val evaluation =
            try {
                dism.runtime
                    .dynamicSchedule()
                    .preview(
                        model = model,
                        now = now,
                    )
            } catch (error: Exception) {
                val message =
                    error.message
                        ?: "Failed to evaluate the schedule."

                return DismUiState(
                    today =
                        HomeUiState(
                            dateLabel = "",
                            errorMessage = message,
                        ),
                    schedule =
                        ScheduleUiState(
                            errorMessage = message,
                        ),
                    attention =
                        AttentionUiState(
                            errorMessage = message,
                        ),
                    isLoading = false,
                )
            }

        /*
         * 决策发生变化时补记一次。
         *
         * 位置很关键：在 `preview` 之后、投影之前。
         *
         * 顺序不能反过来——如果先 evaluate 再 preview，
         * 就变成"每次都记录"了（那正是修掉的老问题）。
         * 这里必须先算出结果、与已记录的比对，确认真的变了才落盘。
         */
        val recordingError =
            recordHistoryIfChanged(model = model, now = now)

        return DismUiState(
            today =
                me.moiclathy.dism.presentation.home.homeUiState(
                    model = model,
                    evaluation = evaluation,
                    now = now,
                    timeZone = timeZone,
                ),

            schedule =
                me.moiclathy.dism.presentation.schedule
                    .scheduleUiState(
                        model = model,
                        evaluation = evaluation,
                        timeZone = timeZone,
                    ),

            attention =
                me.moiclathy.dism.presentation.attention
                    .attentionUiState(
                        model = model,
                        evaluation = evaluation,
                        timeZone = timeZone,
                    ),


            history =
                loadHistory(
                    model = model,
                    itemId =
                        firstItemIdOf(model),
                    recordingError = recordingError,
                ),

            settings =
                me.moiclathy.dism.presentation.settings
                    .settingsUiState(dism = dism),

            isLoading = false,
        )
    }

    /**
     * 16.22：决策变了才记录。
     *
     * ### 为什么这里吞掉异常（而别处不吞）
     *
     * 这是一个**有意的降级**，代价明确：
     *
     * ```text
     * 记录失败  →  History 页少一条
     * 抛出      →  整个 Today 页变成错误页
     * ```
     *
     * "看不了历史"远好于"看不了日程"。
     * 主功能不该被一个附属写入的失败带崩。
     *
     * 对比 [me.moiclathy.dism.calendar.ImportedCalendarStore.save]：
     * 那里**不能**吞，因为吞掉会让用户看到"导入成功"、
     * 重启后数据没了——那是欺骗。
     * 这里的失败不会让用户看到任何假象，只是历史少了记录。
     *
     * ### 已知不足的现状
     *
     * 失败会发一条 `WARN` 诊断事件（code `DECISION_RECORD_FAILED`），
     * 而不是被完全吞掉。
     *
     * ```text
     * 吞掉且不报告  →  历史悄悄不长了，查不出来
     * 吞掉并报告    →  页面照常可用，诊断里有据可查   ← 现在
     * ```
     *
     * 这就是"有意降级"与"静默失败"的区别。
     *
     * 16.26：失败也会**回传给界面**，不只是进诊断。
     *
     * 诊断是开发者的通道。如果只有它，
     * 用户看到的依旧是一个空 History 页，没有任何解释——
     * 而"页面看起来正常但内容是错的"是最难发现的一类问题。
     *
     * @return 失败原因；成功或没变化时为 null
     */
    private suspend fun recordHistoryIfChanged(
        model:
            me.moiclathy.dism.schedule.model.DynamicScheduleModel,
        now: Instant,
    ): String? =
        try {
            recordDecision(model, now)
            null
        } catch (error: Exception) {
            diagnostics?.emit(
                me.moiclathy.dism.diagnostics
                    .decisionRecordFailedEvent(error),
            )

            error.message
                ?: error::class.simpleName
                ?: "unknown error"
        }

    /**
     * History 是按 item 查询的（见 `HistoryUiState` 的说明）。
     *
     * 页面首次打开时默认选中第一条，用户再切换。
     *
     * ### 为什么不去 `latest()` 而直接用 `query()`
     *
     * 这一页要的是"这条的历史"，而不只是"最近一次"。
     * `latest()` 只用在一处：如果以后要显示"最后决策于何时"。
     */
    private suspend fun loadHistory(
        model: me.moiclathy.dism.schedule.model.DynamicScheduleModel,
        itemId: String?,
        recordingError: String?,
    ): HistoryUiState {

        val items =
            me.moiclathy.dism.presentation.history
                .historyItems(model)

        if (itemId == null) {
            return HistoryUiState(
                items = items,
                isLoading = false,
                recordingError = recordingError,
            )
        }

        val result =
            try {
                dism.runtime
                    .dynamicSchedule()
                    .history
                    .query(
                        itemId =
                            me.moiclathy.dism.schedule.model
                                .DynamicScheduleItemId(itemId),
                    )
            } catch (error: Exception) {
                return HistoryUiState(
                    items = items,
                    selectedItemId = itemId,
                    errorMessage =
                        error.message
                            ?: "Failed to load history.",
                    recordingError = recordingError,
                )
            }

        return HistoryUiState(
            items = items,

            selectedItemId = itemId,

            entries =
                me.moiclathy.dism.presentation.history
                    .historyEntries(
                        result = result,
                        timeZone = timeZone,
                    ),

            totalMatched = result.totalMatched,

            truncated = result.truncated,

            isLoading = false,

            recordingError = recordingError,
        )
    }

    private fun firstItemIdOf(
        model: me.moiclathy.dism.schedule.model.DynamicScheduleModel,
    ): String? =
        model.items
            .map { it.id.value }
            .minOrNull()
}
