package me.moiclathy.dism.semantic.resolution

import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * 短期、轻量的实体引用记忆。
 *
 * 用于把 resolution 从"单句"推进到"连续语境"：
 * 刚刚明确解析过的实体，几秒后的指代（"那个"）可以靠这里命中。
 *
 * 第一版内存存储即可。
 * 它很可能只是 session / context memory，而不是永久数据库。
 */
class ResolutionMemory {

    private val recentReferences =
        mutableListOf<RecentEntityReference>()

    fun record(
        entityId: Uuid,
        referencedAt: Instant,
        confidence: Double = 1.0,
    ) {
        recentReferences +=
            RecentEntityReference(
                entityId = entityId,
                referencedAt = referencedAt,
                confidence = confidence,
            )
    }

    /**
     * 返回截止到 [now] 的最近引用，按时间从新到旧。
     *
     * 每个实体只保留最近的一次引用（[RecentEntityReference] 已按时间倒序，
     * [distinctBy] 取到的是每个 Entity 最新的那条）。
     *
     * [limit] 限制返回条数。
     */
    fun recent(
        now: Instant,
        limit: Int = 10,
    ): List<RecentEntityReference> {
        require(limit >= 0)

        return recentReferences
            .asSequence()
            .filter {
                it.referencedAt <= now
            }
            .sortedByDescending {
                it.referencedAt
            }
            .distinctBy {
                it.entityId
            }
            .take(limit)
            .toList()
    }
}
