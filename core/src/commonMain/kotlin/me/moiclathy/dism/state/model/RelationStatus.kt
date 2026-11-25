package me.moiclathy.dism.state.model

/**
 * 当前关系状态。
 */
enum class RelationStatus {

    /**
     * 当前仍被认为成立。
     */
    ACTIVE,

    /**
     * 已被更新证据取代。
     */
    SUPERSEDED,

    /**
     * 明确被用户或可靠证据否定。
     */
    INVALIDATED,
}