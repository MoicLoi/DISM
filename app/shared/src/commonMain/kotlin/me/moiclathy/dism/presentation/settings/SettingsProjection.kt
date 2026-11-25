package me.moiclathy.dism.presentation.settings

import me.moiclathy.dism.api.Dism
import me.moiclathy.dism.api.DismCapabilities

/**
 * 16.9：`Dism -> SettingsUiState` 的纯映射。
 *
 * 每一项的描述都写清"它到底意味着什么能力"，
 * 而不是把字段名翻译一遍（"Dynamic Schedule" → "动态调度"）。
 * 后者对用户没有任何信息量。
 */
fun settingsUiState(
    dism: Dism,
): SettingsUiState =
    SettingsUiState(
        apiVersion = dism.apiVersion,
        capabilities = capabilityRows(dism.capabilities),
        isLoading = false,
    )

private fun capabilityRows(
    capabilities: DismCapabilities,
): List<CapabilityUi> =
    listOf(
        CapabilityUi(
            name = "Semantic state",
            description =
                "Tracks entities, relations and the evidence " +
                    "behind them.",
            enabled = capabilities.semanticState,
        ),
        CapabilityUi(
            name = "Dynamic schedule",
            description =
                "Evaluates schedules, replays history and " +
                    "verifies results.",
            enabled = capabilities.dynamicSchedule,
        ),
        CapabilityUi(
            name = "Attention",
            description =
                "Decides what is worth interrupting you for.",
            enabled = capabilities.attention,
        ),
        CapabilityUi(
            name = "Persistence",
            description =
                "Keeps decisions across restarts.",
            enabled = capabilities.persistence,
        ),
        CapabilityUi(
            name = "Replay",
            description =
                "Re-runs a past decision from its stored input.",
            enabled = capabilities.replay,
        ),
        CapabilityUi(
            name = "Verification",
            description =
                "Compares a replay against the historical result.",
            enabled = capabilities.verification,
        ),
        CapabilityUi(
            name = "Adaptation",
            description =
                "Lets past behaviour shift future weights, " +
                    "within a bounded range.",
            enabled = capabilities.adaptation,
        ),
        CapabilityUi(
            name = "Local first",
            description =
                "Everything works without a network connection.",
            enabled = capabilities.localFirst,
        ),
    )
