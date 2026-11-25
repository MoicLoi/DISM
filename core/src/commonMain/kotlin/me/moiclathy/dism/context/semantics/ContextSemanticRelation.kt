package me.moiclathy.dism.context.semantics

/**
 * Context 对目标语义的作用关系。
 */
enum class ContextSemanticRelation {

    /**
     * 当前 Context 明显提高目标相关性。
     */
    RELEVANT,

    /**
     * 当前 Context 对目标有一定帮助。
     */
    SUPPORTING,

    /**
     * 当前 Context 与目标基本无关。
     */
    NEUTRAL,

    /**
     * 当前 Context 降低目标当前相关性。
     */
    IRRELEVANT,

    /**
     * 当前 Context 明显抑制目标。
     */
    SUPPRESSING,
}
