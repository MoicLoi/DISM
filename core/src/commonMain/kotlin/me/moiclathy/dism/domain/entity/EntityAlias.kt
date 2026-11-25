package me.moiclathy.dism.domain.entity

import kotlin.uuid.Uuid

/**
 * 实体的别名。
 *
 * Alias 独立于 Entity 本体建模，
 * 因为它们的生命周期不同：
 *
 * - canonical name = 长期稳定身份
 * - alias = 用户习惯叫法 / 导入词 / 临时称呼
 *
 * 同一个 Entity 可以有多个 Alias，
 * 每个 Alias 带有来源和置信度。
 */
data class EntityAlias(
    val entityId: Uuid,
    val value: String,
    val source: EntityAliasSource,
    val confidence: Double = 1.0,
) {
    init {
        require(value.isNotBlank()) {
            "Entity alias cannot be blank"
        }

        require(confidence in 0.0..1.0) {
            "Alias confidence must be between 0.0 and 1.0"
        }
    }
}
