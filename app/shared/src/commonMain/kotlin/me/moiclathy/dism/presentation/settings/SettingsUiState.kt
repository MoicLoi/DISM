package me.moiclathy.dism.presentation.settings

/**
 * 16.9：Settings 页的状态。
 *
 * ### 第一版只做**只读**能力展示
 *
 * 这一页现在回答的是：
 *
 * ```text
 * 这个构建里到底有什么？
 * ```
 *
 * 而不是"让我改点什么"。
 *
 * 之所以先做只读，是因为可写的设置项需要真正落地到
 * `DismConfiguration` 并影响下次装配——而配置是在
 * `DismFactory.create()` 时一次性生效的，
 * 运行时改一个开关却没有效果，就是"什么都不做的开关"。
 *
 * 等 16.x 做出配置持久化与重启装配之后，这里才有可写项。
 */
data class SettingsUiState(
    val apiVersion: String = "",

    val capabilities: List<CapabilityUi> = emptyList(),

    val isLoading: Boolean = false,

    val errorMessage: String? = null,
) {

    /**
     * 当前构建启用的能力数。
     */
    val enabledCount: Int
        get() = capabilities.count { it.enabled }

    val totalCount: Int
        get() = capabilities.size
}

/**
 * 一项产品能力。
 *
 * 它如实反映 `DismConfiguration.runtime` 的开关
 * （见 `DismCapabilities`）：关掉 attention 之后
 * 这里会显示未启用，而不是永远一片对勾。
 */
data class CapabilityUi(
    val name: String,

    val description: String,

    val enabled: Boolean,
)
