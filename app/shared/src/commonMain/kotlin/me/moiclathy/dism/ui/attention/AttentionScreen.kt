package me.moiclathy.dism.ui.attention

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

import me.moiclathy.dism.presentation.attention.AttentionEntryDetailUi
import me.moiclathy.dism.presentation.attention.AttentionUiState
import me.moiclathy.dism.ui.theme.DismSpacing
import me.moiclathy.dism.ui.pageHeading

/**
 * 16.6：Attention 页。
 *
 * 回答的是"它凭什么提醒我"。
 *
 * ### 没有解释时如实说明，而不是编一句
 *
 * 决策可能判定"需要关注"却没给出 `explanation`。
 * 那种情况下这一页显示：
 *
 * ```text
 * ⚠ 没有给出解释
 * ```
 *
 * 而不是"需要你的注意"。
 *
 * 后者读起来更舒服，但会把
 * [AttentionUiState.unexplainedCount] 这个可观测指标
 * 永久压成 0——于是"有多少提醒无法解释"就再也测不出来了。
 */
@Composable
fun AttentionScreen(
    state: AttentionUiState,
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
                    text = "Attention",
                    modifier = Modifier.pageHeading(),
                    style =
                        MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Text(
                    text = "${state.size} item(s) need your attention",
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
                NothingNeedsAttention()
            }
        }

        items(
            items = state.entries,
            key = { it.itemId },
        ) { entry ->
            AttentionDetailCard(entry)
        }
    }
}

@Composable
private fun AttentionDetailCard(
    entry: AttentionEntryDetailUi,
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                entry.timeLabel?.let {
                    Text(
                        text = it,
                        modifier =
                            Modifier.width(
                                DismSpacing.timeColumn,
                            ),
                        style =
                            MaterialTheme.typography.labelLarge,
                    )
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = entry.title,
                        style =
                            MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )

                    if (entry.reason != null) {
                        Text(
                            text = entry.reason,
                            style =
                                MaterialTheme.typography.bodyMedium,
                        )
                    } else {
                        Text(
                            text = "No explanation was given " +
                                "for this reminder.",
                            style =
                                MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NothingNeedsAttention() {
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
                text = "Nothing needs your attention.",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "DISM only surfaces things that are worth " +
                    "interrupting you for.",
                style = MaterialTheme.typography.bodyMedium,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
