package me.moiclathy.dism.semantic.resolution

/**
 * Entity Resolution 候选的可解释分数构成。
 *
 * 让系统不仅知道"我认为是谁"，
 * 还知道这次判断有多少来自名称、多少来自 alias、多少来自当前上下文。
 *
 * 各分量语义：
 * - lexical: Entity canonical name 带来的匹配分
 * - alias: EntityAlias 带来的匹配分
 * - context: 当前动态语境对最终分的实际贡献
 * - final: 最终用于 Candidate 排序和阈值判断的分数
 *
 * 注意：这里先不放 recency / location / conversation history 等分量，
 * 等真正出现对应 signal 时再加。
 */
data class ResolutionScore(
    /**
     * Entity canonical name 带来的匹配分。
     */
    val lexical: Double = 0.0,

    /**
     * EntityAlias 带来的匹配分。
     */
    val alias: Double = 0.0,

    /**
     * 当前动态语境带来的额外相关性。
     */
    val context: Double = 0.0,

    /**
     * 最终用于 Candidate 排序和阈值判断的分数。
     */
    val final: Double,
) {
    init {
        require(lexical in 0.0..1.0)
        require(alias in 0.0..1.0)
        require(context in 0.0..1.0)
        require(final in 0.0..1.0)
    }
}
