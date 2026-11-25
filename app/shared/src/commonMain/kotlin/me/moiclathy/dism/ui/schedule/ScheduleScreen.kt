package me.moiclathy.dism.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration

import me.moiclathy.dism.presentation.schedule.ScheduleEntryUi
import me.moiclathy.dism.presentation.schedule.ScheduleUiState
import me.moiclathy.dism.ui.theme.DismSpacing
import me.moiclathy.dism.ui.pageHeading

/**
 * 16.5：Schedule 页。
 *
 * ### 与 Today 页的区别
 *
 * ```text
 * Today     今天 + 已经过 Attention 的少量条目（做减法）
 * Schedule  全部条目，含已完成 / 已取消（做加法）
 * ```
 *
 * 所以这里**显示终态条目**，只是把它们淡化：
 * 用户来这一页是为了确认"那件事我确实做完了"，
 * 把它们藏起来反而让人以为丢了。
 *
 * ### 未定时的条目单独成组
 *
 * 如果混在时间轴末尾，它们会变成一堆"没有时间"的条目的堆叠，
 * 看起来像是渲染坏了。单独一个分组标题能让"这些确实没有时间"
 * 这件事变得明确。
 */
@Composable
fun ScheduleScreen(
    state: ScheduleUiState,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding =
            PaddingValues(
                horizontal = DismSpacing.lg,
                vertical = DismSpacing.lg,
            ),
        verticalArrangement =
            Arrangement.spacedBy(DismSpacing.xs),
    ) {
        item {
            Column(
                modifier =
                    Modifier.padding(bottom = DismSpacing.sm),
            ) {
                Text(
                    text = "Schedule",
                    modifier = Modifier.pageHeading(),
                    style =
                        MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Text(
                    text = "${state.size} item(s)",
                    style = MaterialTheme.typography.bodyMedium,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.errorMessage != null) {
            item {
                Text(
                    text = state.errorMessage,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        if (state.isEmpty && !state.isLoading) {
            item {
                EmptySchedule()
            }
        }

        val timed =
            state.entries.filter { it.timeLabel != null }

        val untimed =
            state.unscheduledEntries

        items(
            items = timed,
            key = { it.itemId },
        ) { entry ->
            ScheduleRow(entry)
        }

        if (untimed.isNotEmpty()) {
            item {
                Text(
                    text = "Without a fixed time",
                    modifier =
                        Modifier.padding(
                            top = DismSpacing.lg,
                            bottom = DismSpacing.xs,
                        ),
                    style = MaterialTheme.typography.labelLarge,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            items(
                items = untimed,
                key = { it.itemId },
            ) { entry ->
                ScheduleRow(entry)
            }
        }
    }
}

@Composable
private fun ScheduleRow(
    entry: ScheduleEntryUi,
) {
    /*
     * 终态条目淡化而不是隐藏。
     *
     * 用 alpha 与删除线表达"已经结束"，比直接消失更容易让人确认。
     */
    val contentColor =
        if (entry.isTerminal) {
            MaterialTheme.colorScheme.onSurfaceVariant
        } else {
            MaterialTheme.colorScheme.onSurface
        }

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
                if (entry.needsAttention && !entry.isTerminal) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = entry.title,
                style = MaterialTheme.typography.titleMedium,
                color = contentColor,
                fontWeight =
                    if (entry.needsAttention && !entry.isTerminal) {
                        FontWeight.SemiBold
                    } else {
                        FontWeight.Normal
                    },
                textDecoration =
                    if (entry.isTerminal) {
                        TextDecoration.LineThrough
                    } else {
                        null
                    },
            )

            val details =
                listOfNotNull(
                    entry.rangeLabel,
                    entry.stateLabel,
                    entry.subtitle,
                )

            if (details.isNotEmpty()) {
                Text(
                    text = details.joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EmptySchedule() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = DismSpacing.xxl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Nothing here yet.",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}
