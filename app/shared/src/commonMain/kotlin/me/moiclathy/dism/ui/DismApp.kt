package me.moiclathy.dism.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Badge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import me.moiclathy.dism.calendar.CalendarImportFeedback
import me.moiclathy.dism.presentation.DismUiState
import me.moiclathy.dism.presentation.state.SemanticStateUiState
import me.moiclathy.dism.presentation.navigation.DismDestination
import me.moiclathy.dism.ui.attention.AttentionScreen
import me.moiclathy.dism.ui.history.HistoryScreen
import me.moiclathy.dism.ui.schedule.ScheduleScreen
import me.moiclathy.dism.ui.settings.SettingsScreen
import me.moiclathy.dism.ui.state.SemanticStateScreen
import me.moiclathy.dism.ui.theme.DismSpacing
import me.moiclathy.dism.ui.theme.DismTheme
import me.moiclathy.dism.ui.today.TodayScreen

/**
 * 16.3：导航壳。
 *
 * ### 手机与桌面是两种布局，不是同一套缩放
 *
 * ```text
 * 窄  (maxWidth < 600dp)   底部导航栏，单手可及
 * 宽  (maxWidth >= 600dp)  左侧导轨 + 更大内容区
 * ```
 *
 * 参考文档明确要求"多端不要做成同一个 UI 缩放"。
 * 所以这里用 [BoxWithConstraints] 按**可用宽度**切换布局，
 * 而不是按平台判断——桌面窗口被拉窄时也应该退化成手机布局。
 *
 * ### 只列出已经实现的目的地
 *
 * [DismDestination] 里还有 SEMANTIC_STATE / HISTORY / DIAGNOSTICS，
 * 它们不在导航里，因为对应页面还没做。
 *
 * 放一个点进去只会显示"敬请期待"的入口，比没有入口更糟：
 * 用户会以为功能坏了。等页面做出来再加。
 */
@Composable
fun DismApp(
    state: DismUiState,
    selected: DismDestination,
    onSelect: (DismDestination) -> Unit,
    modifier: Modifier = Modifier,
    importActions: ImportActions = ImportActions(),

    /**
     * 16.31：库级状态**单独传入**。
     *
     * 它不跟着 `DismPresenter` 一起加载（那是四张全表读），
     * 所以由 `App` 在用户选中这一页时按需取来。
     */
    semanticState: SemanticStateUiState =
        SemanticStateUiState(),
) {
    DismTheme {
        BoxWithConstraints(modifier = modifier.fillMaxSize()) {
            /*
             * 16.33：断点是可测的纯函数，不是内联的 `>=`。
             */
            val size =
                DismBreakpoints.windowSizeFor(
                    maxWidth.value.toInt(),
                )

            if (size == DismWindowSize.COMPACT) {
                NarrowLayout(
                    state = state,
                    selected = selected,
                    onSelect = onSelect,
                    importActions = importActions,
                    semanticState = semanticState,
                    size = size,
                )
            } else {
                WideLayout(
                    state = state,
                    selected = selected,
                    onSelect = onSelect,
                    importActions = importActions,
                    semanticState = semanticState,
                    size = size,
                )
            }
        }
    }
}

/**
 * 导入日历相关的界面状态与回调。
 *
 * ### 为什么打成一个包，而不是三个参数
 *
 * 这三个东西只在 Today 页用，但要穿过
 * `DismApp` → `Layout` → `Content` 三层。
 * 拆成三个参数会让每一层都多三个与自己无关的参数，
 * 而且以后每加一个导入相关的状态就要再改三层。
 *
 * ### `onImport == null` 表示这个平台还没接
 *
 * 此时**不渲染** Import 入口。见 [DismApp] 的说明：
 * 一个点了没反应的按钮比没有按钮更糟。
 */
data class ImportActions(
    val onImport: (() -> Unit)? = null,

    val feedback: CalendarImportFeedback? = null,

    val onDismissFeedback: () -> Unit = {},
)

@Composable
private fun NarrowLayout(
    state: DismUiState,
    selected: DismDestination,
    onSelect: (DismDestination) -> Unit,
    importActions: ImportActions,
    semanticState: SemanticStateUiState,
    size: DismWindowSize,
) {
    Scaffold(
        bottomBar = {
            NavigationBar {
                navDestinationsFor(size).forEach { destination ->
                    NavigationBarItem(
                        selected = destination == selected,
                        onClick = { onSelect(destination) },
                        icon = {
                            NavBadge(
                                destination = destination,
                                state = state,
                            )
                        },
                        label = {
                            Text(destination.label())
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
        ) {
            Content(
                state = state,
                destination = selected,
                importActions = importActions,
                semanticState = semanticState,
                size = size,
            )
        }
    }
}

@Composable
private fun WideLayout(
    state: DismUiState,
    selected: DismDestination,
    onSelect: (DismDestination) -> Unit,
    importActions: ImportActions,
    semanticState: SemanticStateUiState,
    size: DismWindowSize,
) {
    Row(modifier = Modifier.fillMaxSize()) {
        NavigationRail(
            modifier = Modifier.fillMaxHeight(),
        ) {
            navDestinationsFor(size).forEach { destination ->
                NavigationRailItem(
                    selected = destination == selected,
                    onClick = { onSelect(destination) },
                    icon = {
                        NavBadge(
                            destination = destination,
                            state = state,
                        )
                    },
                    label = {
                        Text(destination.label())
                    },
                )
            }
        }

        Box(
            modifier =
                Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                    ),
        )

        Box(
            modifier =
                Modifier
                    .fillMaxSize(),
        ) {
            Content(
                state = state,
                destination = selected,
                importActions = importActions,
                semanticState = semanticState,
                size = size,
            )
        }
    }
}

@Composable
private fun Content(
    state: DismUiState,
    destination: DismDestination,
    importActions: ImportActions,
    semanticState: SemanticStateUiState,
    size: DismWindowSize,
) {
    /*
     * 16.33：宽屏上限制内容宽度，避免过长的行。
     *
     * 不限制的话桌面全屏时一行会横跨 2000px 以上，
     * 读下一行时容易丢行。行宽封顶在 840dp（约 45–75 字符），
     * 超出的部分留白居中。
     *
     * 注意这与"窗口宽就多放内容"是两回事：
     *
     * ```text
     * 该随窗口变宽的是   导航形态
     * 不该变宽的是       单栏文字的宽度
     * ```
     *
     * COMPACT 下不加限制——手机宽度本来就远小于上限，
     * 多包一层布局只会让层级更难读。
     */
    if (size == DismWindowSize.COMPACT) {
        ScreenContent(
            state = state,
            destination = destination,
            importActions = importActions,
            semanticState = semanticState,
        )
        return
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Box(
            modifier =
                Modifier
                    .widthIn(
                        max =
                            MAX_CONTENT_WIDTH_DP.dp,
                    ),
        ) {
            ScreenContent(
                state = state,
                destination = destination,
                importActions = importActions,
                semanticState = semanticState,
            )
        }
    }
}

/**
 * 16.33：目的地 → 页面。
 *
 * 与 [Content] 分开，是因为 [Content] 负责"放在什么宽度里"，
 * 这里负责"放哪一页"。两者混在一起时，加一个页面就要
 * 同时想清楚包装层——那是无关的细节。
 */
@Composable
private fun ScreenContent(
    state: DismUiState,
    destination: DismDestination,
    importActions: ImportActions,
    semanticState: SemanticStateUiState,
) {
    when (destination) {
        DismDestination.TODAY ->
            TodayScreen(state = state.today, importActions = importActions)

        DismDestination.SCHEDULE ->
            ScheduleScreen(state = state.schedule)

        DismDestination.ATTENTION ->
            AttentionScreen(state = state.attention)

        DismDestination.SEMANTIC_STATE ->
            SemanticStateScreen(state = semanticState)

        DismDestination.HISTORY ->
            HistoryScreen(state = state.history)

        DismDestination.SETTINGS ->
            SettingsScreen(state = state.settings)

        /*
         * Memory 还没有实现，且**不在导航里**。
         *
         * 保留这个分支的理由：`when` 必须穷举，而且一旦有人
         * 把某个目的地加进导航却忘了接页面，
         * 界面会明确显示这条消息，而不是空白页。
         */
        DismDestination.MEMORY,
        DismDestination.DIAGNOSTICS,
        ->
            NotYetAvailable(destination)
    }
}

@Composable
private fun NotYetAvailable(
    destination: DismDestination,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(DismSpacing.lg),
    ) {
        Text(
            text = destination.label(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )

        Text(
            text = "This screen is not implemented yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}


/**
 * 16.28：导航角标。
 *
 * `badge == null` 时**不渲染任何东西**——不是渲染一个空占位。
 * 导航项本来就没有图标（`icon = {}`），所以这里不能
 * 占用空间，否则每个目的地都会多出一段空白。
 */
@Composable
private fun NavBadge(
    destination: DismDestination,
    state: DismUiState,
) {
    val badge = badgeFor(destination, state)
        ?: return

    val description =
        badgeDescriptionFor(destination, state)

    Box(
        modifier = Modifier.semantics {
            description?.let {
                contentDescription = it
            }
        },
    ) {
        Badge {
            Text(badge)
        }
    }
}

@Composable
private fun DismDestination.label(): String =
    when (this) {
        DismDestination.TODAY -> "Today"
        DismDestination.SCHEDULE -> "Schedule"
        DismDestination.ATTENTION -> "Attention"
        DismDestination.MEMORY -> "Memory"
        DismDestination.SETTINGS -> "Settings"
        DismDestination.SEMANTIC_STATE -> "State"
        DismDestination.HISTORY -> "History"
        DismDestination.DIAGNOSTICS -> "Diagnostics"
    }
