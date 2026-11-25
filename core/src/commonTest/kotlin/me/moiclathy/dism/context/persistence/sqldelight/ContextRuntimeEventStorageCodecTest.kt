package me.moiclathy.dism.context.persistence.sqldelight

import kotlin.test.Test
import kotlin.test.assertEquals

class ContextRuntimeEventStorageCodecTest {

    @Test
    fun emptyMetadataRoundTrips() {

        val original =
            emptyMap<String, String>()

        val encoded =
            ContextRuntimeEventStorageCodec
                .encodeMetadata(original)

        val decoded =
            ContextRuntimeEventStorageCodec
                .decodeMetadata(encoded)

        assertEquals(
            original,
            decoded,
        )
    }

    @Test
    fun metadataRoundTrips() {

        val original =
            mapOf(
                "source" to "schedule",
                "reason" to "class started",
                "confidence" to "0.90",
            )

        val encoded =
            ContextRuntimeEventStorageCodec
                .encodeMetadata(original)

        val decoded =
            ContextRuntimeEventStorageCodec
                .decodeMetadata(encoded)

        assertEquals(
            original,
            decoded,
        )
    }

    @Test
    fun metadataOrderingIsDeterministic() {

        val first =
            mapOf(
                "z" to "last",
                "a" to "first",
                "m" to "middle",
            )

        val second =
            mapOf(
                "m" to "middle",
                "z" to "last",
                "a" to "first",
            )

        val firstEncoded =
            ContextRuntimeEventStorageCodec
                .encodeMetadata(first)

        val secondEncoded =
            ContextRuntimeEventStorageCodec
                .encodeMetadata(second)

        assertEquals(
            firstEncoded,
            secondEncoded,
        )
    }

    @Test
    fun specialCharactersRoundTrip() {

        val original =
            mapOf(
                "reason" to
                    "class:实验课 → 开始",
                "message" to
                    "a:b:c",
                "empty" to "",
            )

        val encoded =
            ContextRuntimeEventStorageCodec
                .encodeMetadata(original)

        val decoded =
            ContextRuntimeEventStorageCodec
                .decodeMetadata(encoded)

        assertEquals(
            original,
            decoded,
        )
    }

    @Test
    fun unicodeRoundTrips() {

        val original =
            mapOf(
                "context" to "上课中",
                "location" to "实验楼一楼",
            )

        val encoded =
            ContextRuntimeEventStorageCodec
                .encodeMetadata(original)

        val decoded =
            ContextRuntimeEventStorageCodec
                .decodeMetadata(encoded)

        assertEquals(
            original,
            decoded,
        )
    }
}
