package me.moiclathy.dism.domain.entity

/**
 * Alias 的来源标记。
 *
 * 不同来源的 alias 可信度不同，
 * 这会影响 Entity Resolution 的评分。
 */
enum class EntityAliasSource {

    /** 用户明确设置的别名。 */
    USER_DEFINED,

    /** 从外部系统导入的别名。 */
    IMPORTED,

    /** 系统根据上下文推断的别名。 */
    INFERRED,

    /** 系统内建的固定别名。 */
    SYSTEM,
}
