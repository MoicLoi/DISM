package me.moiclathy.dism.semantic.observation

/**
 * Observation 的语义模式。
 *
 * 表示外部世界告诉 DISM 的是"某关系成立"还是"某关系不成立"。
 *
 * 不包含 SUPERSEDE / MERGE 等内部行为 ——
 * 这些由 State Engine 自己决定。
 */
enum class ObservationMode {

    /**
     * 观察到某关系成立。
     */
    ASSERT,

    /**
     * 明确观察到某关系不再成立。
     */
    INVALIDATE,
}
