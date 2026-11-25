package me.moiclathy.dism.ui.state

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
import androidx.compose.ui.unit.dp

import me.moiclathy.dism.presentation.state.RelationUi
import me.moiclathy.dism.presentation.state.SemanticStateUiState
import me.moiclathy.dism.ui.theme.DismSpacing
import me.moiclathy.dism.ui.pageHeading

/**
 * 16.7：Semantic State 页（桌面 / Web）。
 *
 * 回答"DISM 现在认为世界是什么样的"。
 *
 * ### 计数为什么放在最上面
 *
 * 用户第一眼要的是量级感：
 *
 * ```text
 * 多少实体 / 多少证据 / 多少状态 / 多少转换
 * ```
 *
 * 尤其是 [SemanticStateUiState.activeRelationCount] 与
 * [SemanticStateUiState.relationCount] 的差别——
 * "DISM 记得多少"与"DISM 现在相信多少"不是一回事，
 * 那正是 Semantic State 与普通数据库表的区别。
 */
@Composable
fun SemanticStateScreen(
    state: SemanticStateUiState,
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
                    text = "Semantic State",
                    modifier = Modifier.pageHeading(),
                    style =
                        MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Text(
                    text =
                        "${state.activeRelationCount} active / " +
                            "${state.relationCount} tracked relation(s)",
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

        item {
            CountRow(state)
        }

        /*
         * 16.31：还没读过时不能显示"空"。
         *
         * 这一页现在是**按需加载**的。如果把它当成
         * "已经读了、结果为空"，用户会看到
         * "DISM 什么都还不知道"——而事实是"还没去读"。
         *
         * 把一个尚未发生的读取显示成事实，就是撒谎。
         */
        if (!state.isLoaded) {
            item {
                LoadingState()
            }
        } else if (state.isEmpty && !state.isLoading) {
            item {
                EmptyState()
            }
        }

        items(
            items = state.relations,
            key = {
                it.subjectLabel + "|" +
                    it.predicateLabel + "|" +
                    it.objectLabel
            },
        ) { relation ->
            RelationRow(relation)
        }
    }
}

@Composable
private fun CountRow(
    state: SemanticStateUiState,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = DismSpacing.sm),
        horizontalArrangement =
            Arrangement.spacedBy(DismSpacing.xl),
    ) {
        Count("Entities", state.entityCount)
        Count("Evidence", state.evidenceCount)
        Count("Transitions", state.transitionCount)
    }
}

@Composable
private fun Count(
    label: String,
    value: Int,
) {
    Column {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )

        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * 一条关系。
 *
 * 现在的形态是 Uuid 对 Uuid——见 `SemanticStateProjection` 的说明，
 * 这是刻意的诚实而不是半成品：Entity 名字还没接进来，
 * 编一个"Entity 5f3a"只是假装认得它。
 */
@Composable
private fun RelationRow(
    relation: RelationUi,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = DismSpacing.xs),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.width(96.dp),
        ) {
            Text(
                text = relation.subjectLabel,
                style = MaterialTheme.typography.labelLarge,
                color =
                    if (relation.isActive) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                textDecoration =
                    if (relation.isActive) {
                        null
                    } else {
                        TextDecoration.LineThrough
                    },
            )
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = relation.predicateLabel,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )

            Text(
                text =
                    "→ " + relation.objectLabel + " · " +
                        relation.statusLabel + " · " +
                        "confidence " +
                        formatConfidence(relation.confidence) +
                        " · " + relation.supportCount +
                        " evidence",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * 两位小数的置信度。
 *
 * `toString()` 会给出 `0.8333333333333334` 这种数字，
 * 在列表里既占地方又假精确。
 */
private fun formatConfidence(
    value: Double,
): String {
    val rounded =
        (value * 100.0).toInt()

    return (rounded / 100).toString() + "." +
        ((rounded % 100) / 10).toString() +
        ((rounded % 10)).toString()
}

/**
 * 正在读取库级状态。
 *
 * 与 `EmptyState` 严格区分：那一个说的是"读过了，确实没有"，
 * 这一个说的是"还没读"。两者对用户的含义完全不同。
 */
@Composable
private fun LoadingState() {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = DismSpacing.xxl),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Reading the library state…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun EmptyState() {
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
                text = "DISM does not know anything yet.",
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
            )

            Text(
                text = "Relations show up here after observations " +
                    "are recorded.",
                style = MaterialTheme.typography.bodyMedium,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}
