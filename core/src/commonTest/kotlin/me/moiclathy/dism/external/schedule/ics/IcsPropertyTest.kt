package me.moiclathy.dism.external.schedule.ics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IcsPropertyTest {

    @Test
    fun simplePropertyShouldParse() {

        val property =
            IcsProperty.parse(
                "SUMMARY:高等数学"
            )

        assertEquals(
            "SUMMARY",
            property.name,
        )

        assertEquals(
            "高等数学",
            property.value,
        )
    }

    @Test
    fun propertyWithParameterShouldParse() {

        val property =
            IcsProperty.parse(
                "DTSTART;VALUE=DATE:20260916"
            )

        assertEquals(
            "DTSTART",
            property.name,
        )

        assertEquals(
            "DATE",
            property["VALUE"],
        )

        assertEquals(
            "20260916",
            property.value,
        )
    }

    @Test
    fun invalidPropertyShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            IcsProperty.parse(
                "INVALID"
            )
        }
    }
}
