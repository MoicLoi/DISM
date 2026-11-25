package me.moiclathy.dism.semantic.resolution

import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityAlias
import me.moiclathy.dism.persistence.repository.EntityRepository

/**
 * 将外部世界的名字映射到内部持续存在的实体。
 *
 * 第一版使用确定性名字匹配。
 * 后续可以扩展为 embedding / 编辑距离 / LLM / 拼音 / 中文分词。
 *
 * 设计原则：
 * > 不确定的时候要保留不确定，而不是伪造确定性。
 *
 * Score 结构（[ResolutionScore]）：
 * - lexical: canonical name 匹配
 * - alias: EntityAlias 匹配
 * - context: 当前上下文对最终分的实际贡献
 * - final: 用于排序与阈值判断的最终分
 *
 * 两条解析路径：
 * - NAMED（"充电宝"）→ lexical + context 作为辅助（× 0.20）
 * - REFERENCE（"那个"）→ context-only，context 作为主要证据（× 0.95）
 *
 * 职责边界：
 * Resolver 只消费 "当前上下文告诉我这个 Entity 有多少 relevance"，
 * 不关心 relevance 来自当前活动 / upcoming / 最近提及 / 当前地点。
 * 那些属于 Context 层的职责。
 */
class EntityResolver {

    fun resolve(
        mention: EntityMention,
        registry: EntityRepository,
        context: ResolutionContext =
            ResolutionContext(),
    ): EntityResolutionResult {

        val candidates =
            registry.all()
                .asSequence()
                .filter { entity ->
                    mention.expectedType == null ||
                        entity.type ==
                        mention.expectedType
                }
                .mapNotNull { entity ->

                    val contextRelevance =
                        context.entityHints
                            .firstOrNull {
                                it.entityId == entity.id
                            }
                            ?.relevance
                            ?: 0.0

                    scoreCandidate(
                        mention = mention,
                        entity = entity,
                        aliases =
                            registry.aliasesFor(
                                entity.id
                            ),
                        contextRelevance =
                            contextRelevance,
                    )
                }
                .sortedByDescending {
                    it.score.final
                }
                .toList()

        if (candidates.isEmpty()) {
            return EntityResolutionResult.Unresolved
        }

        val best = candidates.first()
        val second = candidates.getOrNull(1)

        return when {
            best.score.final >= 0.95 &&
                (second == null ||
                    best.score.final -
                    second.score.final >=
                    0.15) ->
                EntityResolutionResult.Resolved(best)

            best.score.final >= 0.60 ->
                EntityResolutionResult.Ambiguous(
                    candidates = candidates.take(5),
                )

            else ->
                EntityResolutionResult.Unresolved
        }
    }

    private fun scoreCandidate(
        mention: EntityMention,
        entity: Entity,
        aliases: List<EntityAlias>,
        contextRelevance: Double,
    ): EntityResolutionCandidate? {

        val normalizedMention =
            normalize(mention.text)

        val lexicalScore =
            scoreTextMatch(
                mention = normalizedMention,
                candidate = entity.name,
                exactScore = 1.0,
                partialScore = 0.75,
            ) ?: 0.0

        val bestAliasMatch =
            aliases
                .mapNotNull { alias ->

                    val textScore =
                        scoreTextMatch(
                            mention = normalizedMention,
                            candidate = alias.value,
                            exactScore = 0.98,
                            partialScore = 0.72,
                        ) ?: return@mapNotNull null

                    alias to (
                        textScore * alias.confidence
                        )
                }
                .maxByOrNull {
                    it.second
                }

        val aliasScore =
            bestAliasMatch
                ?.second
                ?: 0.0

        val baseScore =
            maxOf(
                lexicalScore,
                aliasScore,
            )

        // Context-only resolution 只允许 REFERENCE 提及：
        // 陌生词（NAMED）不能靠强上下文被强行解析。
        if (
            baseScore <= 0.0 &&
            (
                mention.kind !=
                    EntityMentionKind.REFERENCE ||
                    contextRelevance <= 0.0
                )
        ) {
            return null
        }

        // 两种模式：
        // 有 lexical 支撑 → context 只是辅助（× 0.20）
        // 无 lexical 但明显指代（REFERENCE）→ context 是主要证据（× 0.95）
        val contextContribution =
            if (baseScore > 0.0) {
                contextRelevance * 0.20
            } else {
                contextRelevance * 0.95
            }

        val finalScore =
            (
                baseScore +
                    contextContribution
                )
                .coerceIn(
                    0.0,
                    1.0,
                )

        val reasons =
            buildList {
                if (lexicalScore > 0.0) {
                    add(
                        "Canonical name score: $lexicalScore"
                    )
                }
                if (aliasScore > 0.0) {
                    val alias =
                        bestAliasMatch!!.first
                    add(
                        "Alias score: $aliasScore (${alias.value})"
                    )
                }
                if (contextContribution > 0.0) {
                    add(
                        "Context contribution: $contextContribution"
                    )
                }
            }

        return EntityResolutionCandidate(
            entity = entity,
            score = ResolutionScore(
                lexical = lexicalScore,
                alias = aliasScore,
                context = contextContribution,
                final = finalScore,
            ),
            reasons = reasons,
        )
    }

    private fun scoreTextMatch(
        mention: String,
        candidate: String,
        exactScore: Double,
        partialScore: Double,
    ): Double? {

        val normalizedCandidate =
            normalize(candidate)

        return when {
            mention == normalizedCandidate ->
                exactScore

            normalizedCandidate.contains(
                mention
            ) ||
                mention.contains(
                    normalizedCandidate
                ) ->
                partialScore

            else ->
                null
        }
    }

    private fun normalize(
        value: String,
    ): String {
        return value
            .trim()
            .lowercase()
            .replace(" ", "")
    }
}
