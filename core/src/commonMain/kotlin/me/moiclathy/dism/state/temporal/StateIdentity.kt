package me.moiclathy.dism.state.temporal

import me.moiclathy.dism.state.model.RelationKey
import kotlin.time.Instant

/**
 * 历史状态的唯一标识。
 *
 * 不能用 RelationKey 直接当 identity：
 * 同一关系历史上可能多次成立（Dorm 07:00 / Classroom 10:00 / Dorm 18:00），
 * 所以第一版用 RelationKey + validFrom。
 *
 * 以后如果 RelationState 增加 stateId，这里可以直接换成 stateId。
 */
internal data class StateIdentity(
    val key: RelationKey,
    val validFrom: Instant,
)
