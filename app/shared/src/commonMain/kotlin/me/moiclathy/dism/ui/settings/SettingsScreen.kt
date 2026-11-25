package me.moiclathy.dism.ui.settings

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp

import me.moiclathy.dism.presentation.settings.CapabilityUi
import me.moiclathy.dism.presentation.settings.SettingsUiState
import me.moiclathy.dism.ui.theme.DismSpacing
import me.moiclathy.dism.ui.pageHeading

/**
 * 16.9：Settings 页。
 *
 * ### 第一版是**只读**的，这是刻意的
 *
 * 可写的设置项需要真正影响下次装配。而配置是在
 * `DismFactory.create()` 时一次性生效的：
 *
 * ```text
 * 运行时改一个开关  →  当前 Dism 实例不会变
 *                  →  那就是"什么都不做的开关"
 * ```
 *
 * 与其放一堆改了没反应的开关，不如先如实展示
 * "这个构建里到底有什么"。等 16.x 做出配置持久化与
 * 重启装配之后，这里才会有可编辑项。
 *
 * ### 每一项都注明"它到底意味着什么能力"
 *
 * 不是把字段名翻译一遍（"Dynamic Schedule" → "动态调度"），
 * 后者对用户没有任何信息量。
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
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
                    text = "Settings",
                    modifier = Modifier.pageHeading(),
                    style =
                        MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.SemiBold,
                )

                Text(
                    text = "DISM ${state.apiVersion}",
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
            Text(
                text =
                    "Capabilities (${state.enabledCount} of " +
                        "${state.totalCount})",
                modifier =
                    Modifier.padding(top = DismSpacing.sm),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }

        items(
            items = state.capabilities,
            key = { it.name },
        ) { capability ->
            CapabilityRow(capability)
        }

        item {
            Text(
                text = "Capabilities are fixed when the runtime " +
                    "is created. Changing them requires a new " +
                    "DismConfiguration, not a switch here.",
                modifier =
                    Modifier.padding(top = DismSpacing.lg),
                style = MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CapabilityRow(
    capability: CapabilityUi,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = DismSpacing.xs),
        verticalAlignment = Alignment.Top,
    ) {
        /*
         * 用文字而不是图标表达启用状态。
         *
         * 图标需要用户知道"对勾是什么意思"，而且颜色在
         * 明暗主题下表现不同；文字没有这个歧义。
         */
        Text(
            text =
                if (capability.enabled) {
                    "on"
                } else {
                    "off"
                },
            modifier = Modifier.width(40.dp),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color =
                if (capability.enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
        )

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = capability.name,
                style = MaterialTheme.typography.titleSmall,
            )

            Text(
                text = capability.description,
                style = MaterialTheme.typography.bodySmall,
                color =
                    MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
