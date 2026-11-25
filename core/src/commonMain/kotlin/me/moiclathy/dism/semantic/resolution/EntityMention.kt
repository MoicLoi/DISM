package me.moiclathy.dism.semantic.resolution

import me.moiclathy.dism.domain.entity.EntityType

/**
 * 外部世界对实体的提及（mention）。
 *
 * Mention 本质是"外部世界说了一个名字，但还不知道内部到底对应谁"。
 * 不包含 UUID —— 那正是 Resolution 要解决的问题。
 */
data class EntityMention(
    val text: String,

    /**
     * NLP/Provider 如果能判断类型，可以提供。
     * 不确定时保持 null。
     */
    val expectedType: EntityType? = null,

    /**
     * 提及的种类。
     *
     * 默认 NAMED，保持向后兼容。
     * 自然语言解析层负责把 "那个 / 它 / 刚才那个" 判为 REFERENCE。
     */
    val kind: EntityMentionKind = EntityMentionKind.NAMED,
) {
    init {
        require(text.isNotBlank()) {
            "Entity mention text cannot be blank"
        }
    }
}
