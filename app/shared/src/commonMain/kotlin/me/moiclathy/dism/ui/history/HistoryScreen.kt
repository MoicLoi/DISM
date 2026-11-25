package me.moiclathy.dism.ui.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

import me.moiclathy.dism.presentation.history.HistoryEntryUi
import me.moiclathy.dism.presentation.history.HistoryUiState
import me.moiclathy.dism.ui.theme.DismSpacing
import me.moiclathy.dism.ui.pageHeading

/**
 * 16.8：History 页（桌面 / Web）。
 *
 * ### 形态是"选一条 → 看它的历史"
 *
 * 因为公共 API 就是按 item 查询的：
 *
 * ```kotlin
 * history.query(itemId, DynamicScheduleHistoryQuery(limit))
 * ```
 *
 * 没有"查全部历史"。这不是遗漏：全局历史查询会随历史增长
 * 无上限变慢，所以 Core 刻意不给这个入口。
 *
 * ### 截断必须说出来
 *
 * `entries.size < totalMatched` 时页面明确显示
 * "showing N of M"。否则用户会以为一共就只有 N 条，
 * 从而得出"系统只记录过 20 次"的错误结论。
 */
@Composable
fun HistoryScreen(
    state: HistoryUiState,
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
            Arrangement.spacedBy(DismSpacing.sm),
    ) {
        item {
            Column(
                modifier =
                    Modifier.padding(bottom = DismSpacing.sm),
            ) {
                Text(
                    text = "History",
                    modifier = Modifier.pageHeading(),
                    style =
                        MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Text(
                    text =
                        if (state.hasSelection) {
                            "Decisions recorded for " +
                                state.selectedItemId
                        } else {
                            "No item selected"
                        },
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

        /*
         * 16.26：记录失败必须说出来。
         *
         * 放在条目**之前**：它改变的是"下面这些还能不能信"，
         * 而不是某一条的细节。
         */
        if (state.recordingError != null) {
            item {
                RecordingFailureBanner(state.recordingError)
            }
        }

        if (state.truncated) {
            item {
                Text(
                    text =
                        "Showing ${state.entries.size} of " +
                            "${state.totalMatched} recorded " +
                            "decision(s).",
                    style = MaterialTheme.typography.bodySmall,
                    color =
                        MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.isEmpty && !state.isLoading) {
            item {
                EmptyHistory(state)
            }
        }

        items(
            items = state.entries,
            key = { it.itemId + "@" + it.capturedAtLabel },
        ) { entry ->
            HistoryEntryCard(entry)
        }
    }
}

/**
 * 16.26：记录失败的横幅。
 *
 * ### 为什么说得这么直白
 *
 * 记录失败时，下面那张列表**是不完整的**——
 * 而列表本身看不出这一点。用户会把它当成"全部历史"。
 *
 * 所以横幅要回答的不是"出错了"，而是"这张表你不能全信"：
 *
 * ```text
 * 弱说法    Something went wrong.
 * 现在的      明确说历史可能缺条目 + 给出原因
 * ```
 *
 * 原因直接显示出来（例如 SQLITE_READONLY）而不是吞成
 * "未知错误"：用户至少能判断"是不是磁盘满了 / 权限问题"，
 * 而不是只能报一个无法复现的 bug。
 */
@Composable
private fun RecordingFailureBanner(
    reason: String,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.errorContainer,
                contentColor =
                    MaterialTheme.colorScheme.onErrorContainer,
            ),
        shape = RoundedCornerShape(DismSpacing.sm),
    ) {
        Column(
            modifier = Modifier.padding(DismSpacing.md),
        ) {
            Text(
                text = "New decisions could not be recorded.",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                text =
                    "This list may be missing recent entries, " +
                        "so it is not the full story.",
                style = MaterialTheme.typography.bodyMedium,
            )

            Text(
                text = reason,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun HistoryEntryCard(
    entry: HistoryEntryUi,
) {    Card(
        modifier = Modifier.fillMaxWidth(),
        colors =
            CardDefaults.cardColors(
                containerColor =
                    MaterialTheme.colorScheme.surfaceVariant,
                contentColor =
                    MaterialTheme.colorScheme.onSurfaceVariant,
            ),
        shape = RoundedCornerShape(DismSpacing.sm),
    ) {
        Row(
            modifier = Modifier.padding(DismSpacing.md),
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = entry.capturedAtLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                )

                Text(
                    text =
                        "revision ${entry.scheduleRevision} · " +
                            "engine ${entry.engineVersion} · " +
                            traceLabel(entry.hasTrace),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
    }
}

/**
 * Trace 的有无是**可信度**信息，不是技术细节。
 *
 * ```text
 * 有 Trace  →  当时的执行过程可查，能回答"为什么这么算"
 * 没有 Trace →  只剩快照，无法解释
 * ```
 *
 * 所以它用文字明说，而不是用一个图标让人猜。
 */
private fun traceLabel(
    hasTrace: Boolean,
): String =
    if (hasTrace) {
        "trace available"
    } else {
        "no trace"
    }

@Composable
private fun EmptyHistory(
    state: HistoryUiState,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = DismSpacing.xxl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text =
                    if (state.hasSelection) {
                        "No decisions recorded yet."
                    } else {
                        "Nothing to show."
                    },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )

            Text(
                text =
                    "Each evaluation is stored, so history " +
                        "appears after the first one.",
                style = MaterialTheme.typography.bodyMedium,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
