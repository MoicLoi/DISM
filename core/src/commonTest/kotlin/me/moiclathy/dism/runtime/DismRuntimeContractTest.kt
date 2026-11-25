package me.moiclathy.dism.runtime

import kotlin.test.Test
import kotlin.test.assertNotNull

import me.moiclathy.dism.api.DismFactory

class DismRuntimeContractTest {

    @Test
    fun runtimeCanCreateScheduleFacade() {

        val dism =
            DismFactory.create()

        val schedule =
            dism.runtime.dynamicSchedule()

        assertNotNull(
            schedule,
        )
    }

    @Test
    fun runtimeCanProduceSnapshot() {

        val dism =
            DismFactory.create()

        val snapshot =
            dism.runtime.snapshot()

        assertNotNull(
            snapshot,
        )
    }

    @Test
    fun runtimeCanExposeCurrentStates() {

        val dism =
            DismFactory.create()

        val states =
            dism.runtime.currentStates()

        assertNotNull(
            states,
        )
    }
}
