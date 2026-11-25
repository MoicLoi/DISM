package me.moiclathy.dism.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

import me.moiclathy.dism.calendar.CalendarImportFeedback
import me.moiclathy.dism.presentation.home.AttentionEntryUi
import me.moiclathy.dism.presentation.home.HomeUiState
import me.moiclathy.dism.presentation.home.TimelineEntryUi
import me.moiclathy.dism.ui.ImportActions
import me.moiclathy.dism.ui.theme.DismSpacing
import me.moiclathy.dism.ui.pageHeading

/**
 * 16.4：Today 页。
 *
 * ```text
 * Today
 * Wed, Sep 9
 * ────────────────────────
 * 09:00  实验动物学
 *        实验楼201
 *        ↑ 需要提前准备
 *
 * 11:30  午餐
 *        预计 15 min
 * ────────────────────────
 * Attention
 *
 * ⚡ 明天上午需要带充电宝
 * ```
 *
 * ### 这个页面同时验证四件事
 *
 * ```text
 * Dynamic Schedule  →  时间线
 * Attention         →  上半部的"需要准备"
 * Semantic State    →  副标题（当前是 externalKey，16.7 接语义层）
 * Temporal          →  时间标签与排序
 * ```
 *
 * ### 它不认识 Core
 *
 * 这个文件里没有 `Dism`、没有 `DynamicScheduleModel`、
 * 没有 `DynamicScheduleDecisionView`，只有 [HomeUiState]。
 */
@Composable
fun TodayScreen(
    state: HomeUiState,
    modifier: Modifier = Modifier,
    importActions: ImportActions = ImportActions(),
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                horizontal = DismSpacing.lg,
                vertical = DismSpacing.lg,
            ),
        verticalArrangement =
            Arrangement.spacedBy(DismSpacing.sm),
    ) {
        item {
            Header(
                state = state,
                onImport = importActions.onImport,
            )
        }

        importActions.feedback?.let { feedback ->
            item {
                ImportFeedbackBanner(
                    feedback = feedback,
                    onDismiss =
                        importActions.onDismissFeedback,
                )
            }
        }

        if (state.errorMessage != null) {
            item {
                ErrorBanner(state.errorMessage)
            }
        }

        if (state.isEmpty && !state.isLoading) {
            item {
                EmptyState()
            }
        }

        items(
            items = state.timeline,
            key = { it.itemId },
        ) { entry ->
            TimelineRow(entry = entry)
        }

        if (state.attention.isNotEmpty()) {
            item {
                SectionLabel("Attention")
            }

            items(
                items = state.attention,
                key = { "attention:" + it.itemId },
            ) { entry ->
                AttentionCard(entry)
            }
        }
    }
}

@Composable
private fun Header(
    state: HomeUiState,
    onImport: (() -> Unit)?,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(bottom = DismSpacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Today",
                modifier = Modifier.pageHeading(),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = state.dateLabel,
                style = MaterialTheme.typography.bodyMedium,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        /*
         * `onImport == null` 时不渲染按钮。
         *
         * 见 DismApp 的说明：一个点了没反应的入口
         * 比没有入口更糟——用户会以为功能坏了。
         */
        if (onImport != null) {
            TextButton(onClick = onImport) {
                Text("Import")
            }
        }
    }
}

/**
 * 导入结果横幅。
 *
 * ### 为什么把"部分成功"单独说清楚
 *
 * 一个含 3 条好 VEVENT、1 条坏 VEVENT 的文件会导入 3 条。
 * 如果只显示"导入成功"，用户会以为日历是完整的，
 * 从而漏掉那一条——而那可能正是他要的。
 *
 * ```text
 * 全部成功    "Imported 3 events."
 * 部分成功    "Imported 3 of 4 events." + 错误详情
 * 完全失败    "Nothing could be imported." + 错误详情
 * ```
 */
@Composable
private fun ImportFeedbackBanner(
    feedback: CalendarImportFeedback,
    onDismiss: () -> Unit,
) {
    val failed = feedback.importedCount == 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    if (failed) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                contentColor =
                    if (failed) {
                        MaterialTheme.colorScheme.onErrorContainer
                    } else {
                        MaterialTheme.colorScheme.onPrimaryContainer
                    },
            ),
        shape = RoundedCornerShape(DismSpacing.sm),
    ) {
        Column(modifier = Modifier.padding(DismSpacing.md)) {
            Text(
                text =
                    when {
                        failed ->
                            "Nothing could be imported."

                        feedback.isPartial ->
                            "Imported ${feedback.importedCount} " +
                                "event(s), but some were skipped."

                        else ->
                            "Imported ${feedback.importedCount} " +
                                "event(s)."
                    },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )

            feedback.errors.forEach { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            feedback.warnings.forEach { warning ->
                Text(
                    text = warning,
                    style = MaterialTheme.typography.bodySmall,
                )
            }

            TextButton(onClick = onDismiss) {
                Text("Dismiss")
            }
        }
    }
}

/**
 * 时间标签**左对齐固定宽度**，内容在右。
 *
 * 这样整页看起来是一条时间轴；如果让标签自适应宽度，
 * "09:00" 与 "11:30" 会让内容左右抖动。
 */
/**
 * 16.30：**没有点击行为，所以也不是按钮**。
 *
 * 之前这里收一个 `onClick` 参数，但从未把它接到
 * `.clickable` 上——于是产生了两个问题：
 *
 * ```text
 * 1. 从 App 一路传到这里的 onSelectItem 是个死参数，
 *    点了没有任何反应（"什么都不做的开关"）
 * 2. 无障碍层面它既不可聚焦也不可激活，
 *    而调用方以为可以
 * ```
 *
 * 修法不是"把 clickable 接上"：目前**没有详情页**，
 * 接上就是一个点了没反应的按钮——那比现在更糟。
 *
 * 所以整条链路被删掉。等真的有了详情页，
 * 再把它作为一个**有行为**的可点击区域加回来。
 *
 * 这条经验值得记住：无障碍检查往往能顺带照出
 * "界面承诺了但没兑现"的地方。
 */
@Composable
private fun TimelineRow(
    entry: TimelineEntryUi,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = DismSpacing.xs),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = entry.timeLabel ?: "--:--",
            modifier = Modifier.width(DismSpacing.timeColumn),
            style = MaterialTheme.typography.labelLarge,
            color =
                if (entry.needsAttention) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )

        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight =
                    if (entry.needsAttention) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
            )

            entry.rangeLabel?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            entry.subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            entry.explanation?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

/**
 * Attention 是全页唯一使用高饱和容器的区域。
 *
 * 参考文档要求 DISM 的 UI 由注意力驱动而不是由装饰驱动，
 * 那么"强调"这件事就必须稀缺——到处都是强调等于没有强调。
 */
@Composable
private fun AttentionCard(
    entry: AttentionEntryUi,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.primaryContainer,
                contentColor =
                    MaterialTheme.colorScheme.onPrimaryContainer,
            ),
        shape = RoundedCornerShape(DismSpacing.md),
    ) {
        Column(
            modifier = Modifier.padding(DismSpacing.md),
        ) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text = entry.reason,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun SectionLabel(
    text: String,
) {
    Text(
        text = text,
        modifier =
            Modifier.padding(
                top = DismSpacing.lg,
                bottom = DismSpacing.xs,
            ),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ErrorBanner(
    message: String,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .background(
                    color =
                        MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(DismSpacing.sm),
                )
                .padding(DismSpacing.md),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = DismSpacing.xxl),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Nothing scheduled.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )

        Text(
            text = "When there is something to bring or prepare, " +
                "it will show up here.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
