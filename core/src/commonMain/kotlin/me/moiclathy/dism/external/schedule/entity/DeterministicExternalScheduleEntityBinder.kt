package me.moiclathy.dism.external.schedule.entity

import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityRegistry
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.external.schedule.normalization.NormalizedExternalScheduleEntry

/**
 * 确定性的 External Schedule Entity Binder。
 *
 * 当前只使用：
 *
 * 1. Entity.name
 * 2. EntityAlias
 *
 * 不进行模糊匹配。
 */
class DeterministicExternalScheduleEntityBinder(
    private val policy:
        ExternalScheduleEntityBindingPolicy =
        ExternalScheduleEntityBindingPolicy(),
) : ExternalScheduleEntityBinder {

    override fun bind(
        entry: NormalizedExternalScheduleEntry,
        entityRegistry: EntityRegistry,
    ): ExternalScheduleEntityBindingResult {

        val bindings =
            mutableListOf<ExternalScheduleEntityBinding>()

        val candidates =
            mutableListOf<ExternalScheduleEntityBindingCandidate>()

        val warnings =
            mutableListOf<String>()

        bindCourse(
            entry = entry,
            registry = entityRegistry,
            bindings = bindings,
            candidates = candidates,
            warnings = warnings,
        )

        bindInstructor(
            entry = entry,
            registry = entityRegistry,
            bindings = bindings,
            candidates = candidates,
            warnings = warnings,
        )

        bindLocation(
            entry = entry,
            registry = entityRegistry,
            bindings = bindings,
            candidates = candidates,
            warnings = warnings,
        )

        return ExternalScheduleEntityBindingResult(
            bindings =
                bindings,

            candidates =
                candidates,

            warnings =
                warnings,
        )
    }

    private fun bindCourse(
        entry: NormalizedExternalScheduleEntry,
        registry: EntityRegistry,
        bindings: MutableList<ExternalScheduleEntityBinding>,
        candidates: MutableList<ExternalScheduleEntityBindingCandidate>,
        warnings: MutableList<String>,
    ) {

        if (!policy.bindCourse) {
            return
        }

        val value =
            entry.courseCode
                ?: entry.title

        val entity =
            findEntity(
                value = value,
                expectedType = EntityType.COURSE,
                registry = registry,
                candidates = candidates,
            )

        if (entity == null) {
            warnings +=
                "No COURSE Entity matched '$value' for entry ${entry.id}."

            return
        }

        bindings +=
            ExternalScheduleEntityBinding(
                sourceEntryId =
                    entry.id,

                bindingType =
                    ExternalScheduleEntityBindingType.COURSE,

                entity =
                    entity,

                sourceValue =
                    value,

                confidence =
                    1.0,

                matchedBy =
                    "exact-name-or-alias",
            )
    }

    private fun bindInstructor(
        entry: NormalizedExternalScheduleEntry,
        registry: EntityRegistry,
        bindings: MutableList<ExternalScheduleEntityBinding>,
        candidates: MutableList<ExternalScheduleEntityBindingCandidate>,
        warnings: MutableList<String>,
    ) {

        if (!policy.bindInstructor) {
            return
        }

        val value =
            entry.instructor
                ?: return

        val entity =
            findEntity(
                value = value,
                expectedType = EntityType.PERSON,
                registry = registry,
                candidates = candidates,
            )

        if (entity == null) {
            warnings +=
                "No PERSON Entity matched '$value' for entry ${entry.id}."

            return
        }

        bindings +=
            ExternalScheduleEntityBinding(
                sourceEntryId =
                    entry.id,

                bindingType =
                    ExternalScheduleEntityBindingType.INSTRUCTOR,

                entity =
                    entity,

                sourceValue =
                    value,

                confidence =
                    1.0,

                matchedBy =
                    "exact-name-or-alias",
            )
    }

    private fun bindLocation(
        entry: NormalizedExternalScheduleEntry,
        registry: EntityRegistry,
        bindings: MutableList<ExternalScheduleEntityBinding>,
        candidates: MutableList<ExternalScheduleEntityBindingCandidate>,
        warnings: MutableList<String>,
    ) {

        if (!policy.bindLocation) {
            return
        }

        val value =
            entry.location
                ?: return

        val entity =
            findEntity(
                value = value,
                expectedType = EntityType.LOCATION,
                registry = registry,
                candidates = candidates,
            )

        if (entity == null) {
            warnings +=
                "No LOCATION Entity matched '$value' for entry ${entry.id}."

            return
        }

        bindings +=
            ExternalScheduleEntityBinding(
                sourceEntryId =
                    entry.id,

                bindingType =
                    ExternalScheduleEntityBindingType.LOCATION,

                entity =
                    entity,

                sourceValue =
                    value,

                confidence =
                    1.0,

                matchedBy =
                    "exact-name-or-alias",
            )
    }

    private fun findEntity(
        value: String,
        expectedType: EntityType,
        registry: EntityRegistry,
        candidates: MutableList<ExternalScheduleEntityBindingCandidate>,
    ): Entity? {

        val normalizedValue =
            normalizeLookupValue(
                value
            )

        /**
         * 第一层：
         *
         * Entity.name 精确匹配。
         */
        val directMatches =
            registry
                .all()
                .filter {
                    it.type == expectedType
                }
                .filter {
                    normalizeLookupValue(
                        it.name
                    ) == normalizedValue
                }

        if (directMatches.size == 1) {

            val entity =
                directMatches.first()

            candidates +=
                ExternalScheduleEntityBindingCandidate(
                    entity =
                        entity,

                    bindingType =
                        bindingTypeFor(
                            expectedType
                        ),

                    sourceValue =
                        value,

                    score =
                        1.0,
                )

            return entity
        }

        /**
         * 多个同名 Entity：
         *
         * 不猜。
         */
        if (directMatches.size > 1) {

            directMatches.forEach { entity ->

                candidates +=
                    ExternalScheduleEntityBindingCandidate(
                        entity =
                            entity,

                        bindingType =
                            bindingTypeFor(
                                expectedType
                            ),

                        sourceValue =
                            value,

                        score =
                            1.0,
                    )
            }

            return null
        }

        if (!policy.useAliases) {
            return null
        }

        /**
         * 第二层：
         *
         * EntityAlias。
         *
         * 这里依赖 EntityRegistry 已有的 aliasesFor / allAliases API。
         */
        val aliasMatches =
            registry
                .all()
                .filter {
                    it.type == expectedType
                }
                .filter { entity ->

                    registry
                        .aliasesFor(
                            entity.id
                        )
                        .any { alias ->

                            normalizeLookupValue(
                                alias.value
                            ) == normalizedValue
                        }
                }

        if (aliasMatches.size == 1) {

            val entity =
                aliasMatches.first()

            candidates +=
                ExternalScheduleEntityBindingCandidate(
                    entity =
                        entity,

                    bindingType =
                        bindingTypeFor(
                            expectedType
                        ),

                    sourceValue =
                        value,

                    score =
                        1.0,
                )

            return entity
        }

        /**
         * 多个 Alias 指向不同 Entity：
         *
         * 同样不猜。
         */
        aliasMatches.forEach { entity ->

            candidates +=
                ExternalScheduleEntityBindingCandidate(
                    entity =
                        entity,

                    bindingType =
                        bindingTypeFor(
                            expectedType
                        ),

                    sourceValue =
                        value,

                    score =
                        1.0,
                )
        }

        return null
    }

    private fun bindingTypeFor(
        entityType: EntityType,
    ): ExternalScheduleEntityBindingType =
        when (entityType) {

            EntityType.COURSE ->
                ExternalScheduleEntityBindingType.COURSE

            EntityType.PERSON ->
                ExternalScheduleEntityBindingType.INSTRUCTOR

            EntityType.LOCATION ->
                ExternalScheduleEntityBindingType.LOCATION

            else ->
                throw IllegalArgumentException(
                    "Unsupported EntityType for schedule binding: $entityType"
                )
        }

    private fun normalizeLookupValue(
        value: String,
    ): String =
        value
            .trim()
            .replace(
                Regex("\\s+"),
                " ",
            )
            .lowercase()
}
