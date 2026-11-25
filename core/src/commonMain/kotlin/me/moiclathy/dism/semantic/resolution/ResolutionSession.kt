package me.moiclathy.dism.semantic.resolution

import me.moiclathy.dism.persistence.repository.EntityRepository
import kotlin.time.Instant

/**
 * 有状态的连续解析过程。
 *
 * 把 "成功解析后写入最近引用记忆" 自动化：
 *
 * ResolutionSession.resolve(...)
 *     ↓
 * EntityResolver
 *     ↓
 * Resolved
 *     ↓ 自动写入 ResolutionMemory
 *
 * 之后 "白色充电宝" → Resolved → 自动记住，
 * "那个" → REFERENCE → ResolutionContextBuilder 读取最近引用 → Resolved 到同一 Entity。
 *
 * 边界：
 * - EntityResolver = 纯解析逻辑（可单元测试 / 批量离线 / provider import / replay）
 * - ResolutionSession = 有状态的连续解析过程（聊天式连续语境）
 *
 * 安全性质：
 * 只有真正 Resolved 的实体才写入记忆；
 * Ambiguous / Unresolved 不写入，避免弱猜测自我强化的反馈污染。
 *
 * 事务语义：
 * [resolve] 是"解析 + 自动提交"的便捷入口；
 * 需要"全成功后再提交"的场景（如 Observation 的 subject/object）
 * 可以先用 [resolveWithoutRecording] 暂存，全部成功后再用 [recordResolved] 提交。
 */
class ResolutionSession(
    private val resolver: EntityResolver =
        EntityResolver(),

    private val memory: ResolutionMemory =
        ResolutionMemory(),
) {

    fun resolve(
        mention: EntityMention,
        registry: EntityRepository,
        context: ResolutionContext,
        resolvedAt: Instant,
    ): EntityResolutionResult {

        val result =
            resolveWithoutRecording(
                mention = mention,
                registry = registry,
                context = context,
            )

        if (
            result is
            EntityResolutionResult.Resolved
        ) {
            recordResolved(
                result = result,
                resolvedAt = resolvedAt,
            )
        }

        return result
    }

    /**
     * 只解析，不写入记忆。
     *
     * 用于事务式场景：先 provisional 解析，
     * 只有整个业务单元成功后才 [recordResolved]。
     */
    fun resolveWithoutRecording(
        mention: EntityMention,
        registry: EntityRepository,
        context: ResolutionContext,
    ): EntityResolutionResult {
        return resolver.resolve(
            mention = mention,
            registry = registry,
            context = context,
        )
    }

    /**
     * 把一次已成功的解析写入记忆。
     *
     * 置信度直接继承最终分：
     * exact name → 1.0
     * alias → 0.98
     * context reference → 0.95
     * 后续 recent relevance 自然继承这个置信度。
     */
    fun recordResolved(
        result: EntityResolutionResult.Resolved,
        resolvedAt: Instant,
    ) {
        memory.record(
            entityId =
                result.candidate.entity.id,
            referencedAt =
                resolvedAt,
            confidence =
                result.candidate.score.final,
        )
    }

    fun memory(): ResolutionMemory {
        return memory
    }
}
