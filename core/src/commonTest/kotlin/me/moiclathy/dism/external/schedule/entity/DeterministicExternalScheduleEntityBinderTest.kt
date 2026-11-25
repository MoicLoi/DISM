package me.moiclathy.dism.external.schedule.entity

import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityRegistry
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.normalization.NormalizedExternalScheduleEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeterministicExternalScheduleEntityBinderTest {

    private val entry =
        NormalizedExternalScheduleEntry(
            id =
                ExternalScheduleId(
                    "entry-001"
                ),

            title =
                "高等数学",

            startAt =
                null,

            endAt =
                null,

            location =
                "A301",

            instructor =
                "张老师",

            courseCode =
                "MATH101",

            description =
                null,
        )

    private fun registry(): EntityRegistry {

        val registry =
            EntityRegistry()

        registry.register(
            Entity(
                type =
                    EntityType.COURSE,

                name =
                    "MATH101",
            )
        )

        registry.register(
            Entity(
                type =
                    EntityType.PERSON,

                name =
                    "张老师",
            )
        )

        registry.register(
            Entity(
                type =
                    EntityType.LOCATION,

                name =
                    "A301",
            )
        )

        return registry
    }

    @Test
    fun courseShouldBeBound() {

        val result =
            DeterministicExternalScheduleEntityBinder()
                .bind(
                    entry =
                        entry,

                    entityRegistry =
                        registry(),
                )

        assertTrue(
            result.success
        )

        val binding =
            result.bindings
                .first {
                    it.bindingType ==
                        ExternalScheduleEntityBindingType.COURSE
                }

        assertEquals(
            "MATH101",
            binding.entity.name,
        )
    }

    @Test
    fun instructorShouldBeBound() {

        val result =
            DeterministicExternalScheduleEntityBinder()
                .bind(
                    entry,
                    registry(),
                )

        val binding =
            result.bindings
                .first {
                    it.bindingType ==
                        ExternalScheduleEntityBindingType.INSTRUCTOR
                }

        assertEquals(
            "张老师",
            binding.entity.name,
        )
    }

    @Test
    fun locationShouldBeBound() {

        val result =
            DeterministicExternalScheduleEntityBinder()
                .bind(
                    entry,
                    registry(),
                )

        val binding =
            result.bindings
                .first {
                    it.bindingType ==
                        ExternalScheduleEntityBindingType.LOCATION
                }

        assertEquals(
            "A301",
            binding.entity.name,
        )
    }

    @Test
    fun allThreeBindingsShouldBeProduced() {

        val result =
            DeterministicExternalScheduleEntityBinder()
                .bind(
                    entry,
                    registry(),
                )

        assertEquals(
            3,
            result.bindings.size,
        )
    }

    @Test
    fun missingEntityShouldProduceWarningNotError() {

        val registry =
            EntityRegistry()

        val result =
            DeterministicExternalScheduleEntityBinder()
                .bind(
                    entry,
                    registry,
                )

        assertTrue(
            result.success
        )

        assertEquals(
            0,
            result.bindings.size,
        )

        assertTrue(
            result.warnings.isNotEmpty()
        )
    }

    @Test
    fun aliasShouldBeSupported() {

        val registry =
            EntityRegistry()

        val course =
            Entity(
                type =
                    EntityType.COURSE,

                name =
                    "MATH101",
            )

        registry.register(
            course
        )

        registry.registerAlias(
            me.moiclathy.dism.domain.entity.EntityAlias(
                entityId =
                    course.id,

                value =
                    "高等数学",

                source =
                    me.moiclathy.dism.domain.entity.EntityAliasSource.IMPORTED,

                confidence =
                    1.0,
            )
        )

        val result =
            DeterministicExternalScheduleEntityBinder()
                .bind(
                    entry,
                    registry,
                )

        val binding =
            result.bindings
                .firstOrNull {
                    it.bindingType ==
                        ExternalScheduleEntityBindingType.COURSE
                }

        assertEquals(
            "MATH101",
            binding?.entity?.name,
        )
    }

    @Test
    fun wrongEntityTypeShouldNotBind() {

        val registry =
            EntityRegistry()

        registry.register(
            Entity(
                type =
                    EntityType.ITEM,

                name =
                    "MATH101",
            )
        )

        val result =
            DeterministicExternalScheduleEntityBinder()
                .bind(
                    entry,
                    registry,
                )

        assertTrue(
            result.bindings.none {
                it.bindingType ==
                    ExternalScheduleEntityBindingType.COURSE
            }
        )
    }

    @Test
    fun disabledBindingShouldNotProduceBinding() {

        val policy =
            ExternalScheduleEntityBindingPolicy(
                bindCourse =
                    false,

                bindInstructor =
                    false,

                bindLocation =
                    false,
            )

        val result =
            DeterministicExternalScheduleEntityBinder(
                policy
            ).bind(
                entry,
                registry(),
            )

        assertEquals(
            0,
            result.bindings.size,
        )
    }
}
