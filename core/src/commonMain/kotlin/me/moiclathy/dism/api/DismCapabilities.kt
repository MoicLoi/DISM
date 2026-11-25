package me.moiclathy.dism.api

/**
 * DISM 产品能力声明。
 *
 * 只描述**已经稳定存在**的公共能力，
 * 不代表所有内部实验性模块。
 *
 * ### 为什么要有它
 *
 * 平台层（Desktop / Android / iOS / Server）在装配 UI 时
 * 经常需要问"这个构建里到底有没有 X"。没有声明的话，
 * 只能靠版本号猜，而版本号不表达能力。
 *
 * 注意这里的每一项都对应一条**已经被测试覆盖**的链路，
 * 不是路线图。
 */
@DismPublicApi
data class DismCapabilities(
    /**
     * 语义状态引擎（observe / queryAt / snapshot）。
     */
    val semanticState: Boolean = true,

    /**
     * 动态调度（evaluate / replay / verify）。
     */
    val dynamicSchedule: Boolean = true,

    /**
     * 注意力投影与冲突解析。
     */
    val attention: Boolean = true,

    /**
     * 持久化（含 SQLDelight 实现）。
     */
    val persistence: Boolean = true,

    /**
     * 历史决策重放。
     */
    val replay: Boolean = true,

    /**
     * 重放验证（EXACT_MATCH / BEHAVIOR_CHANGED / ...）。
     */
    val verification: Boolean = true,

    /**
     * 自适应层（14.x：Weight / Attention / Priority /
     * Confidence / Boundary / Override / Provenance / Audit）。
     */
    val adaptation: Boolean = true,

    /**
     * 本地优先：全部能力在没有网络的情况下完整可用。
     */
    val localFirst: Boolean = true,
)
