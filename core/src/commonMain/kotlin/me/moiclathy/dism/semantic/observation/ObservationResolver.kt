package me.moiclathy.dism.semantic.observation

import me.moiclathy.dism.persistence.repository.EntityRepository
import me.moiclathy.dism.semantic.resolution.EntityResolutionResult
import me.moiclathy.dism.semantic.resolution.ResolutionContext
import me.moiclathy.dism.semantic.resolution.ResolutionSession

/**
 * 将 SemanticObservationDraft 中的 EntityMention
 * 解析为具体的 Entity UUID，产出 SemanticObservation。
 *
 * 通过 ResolutionSession 获得连续语境：
 * - subject / object 共享同一段短期记忆
 * - 只有整个 Observation 成功解析后，才把实体提交到记忆
 *
 * 事务语义：
 * subject Resolved + object 失败
 * → Observation Unresolved
 * → memory 不变
 *
 * 避免半成功污染：
 * 不会出现 "Observation 没成立，但 subject 已被记成最近实体"。
 */
class ObservationResolver(
    private val resolutionSession: ResolutionSession =
        ResolutionSession(),
) {

    fun resolve(
        draft: SemanticObservationDraft,
        registry: EntityRepository,
        context: ResolutionContext =
            ResolutionContext(),
    ): ObservationResolutionResult {

        val subjectResolution =
            resolutionSession
                .resolveWithoutRecording(
                    mention = draft.subjectMention,
                    registry = registry,
                    context = context,
                )

        val objectResolution =
            resolutionSession
                .resolveWithoutRecording(
                    mention = draft.objectMention,
                    registry = registry,
                    context = context,
                )

        val subject =
            subjectResolution
                as? EntityResolutionResult.Resolved

        val objectEntity =
            objectResolution
                as? EntityResolutionResult.Resolved

        if (
            subject == null ||
            objectEntity == null
        ) {
            return ObservationResolutionResult.Unresolved(
                subjectResolution = subjectResolution,
                objectResolution = objectResolution,
            )
        }

        // 全部成功后才提交，保持记忆一致
        resolutionSession.recordResolved(
            result = subject,
            resolvedAt = draft.observedAt,
        )

        resolutionSession.recordResolved(
            result = objectEntity,
            resolvedAt = draft.observedAt,
        )

        return ObservationResolutionResult.Resolved(
            observation = SemanticObservation(
                subjectId =
                    subject.candidate.entity.id,
                predicate =
                    draft.predicate,
                objectId =
                    objectEntity.candidate.entity.id,
                mode =
                    draft.mode,
                confidence =
                    draft.confidence,
                evidenceIds =
                    draft.evidenceIds,
                observedAt =
                    draft.observedAt,
                effectiveAt =
                    draft.effectiveAt,
            )
        )
    }

    fun session(): ResolutionSession {
        return resolutionSession
    }
}
