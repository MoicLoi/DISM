package me.moiclathy.dism.context.persistence.sqldelight

/**
 * Context Runtime Event 的 SQLite 存储 Codec。
 *
 * metadata:
 *
 * Map<String, String>
 *
 *        ↓
 *
 * String
 *
 *        ↓
 *
 * SQLite TEXT
 *
 * 采用长度前缀，避免普通 delimiter
 * 与用户实际 metadata 内容发生冲突。
 */
object ContextRuntimeEventStorageCodec {

    fun encodeMetadata(
        metadata: Map<String, String>,
    ): String {

        if (metadata.isEmpty()) {
            return ""
        }

        val builder =
            StringBuilder()

        metadata
            .toList()
            .sortedBy {
                it.first
            }
            .forEach { (key, value) ->

                require(key.isNotEmpty()) {
                    "Metadata key must not be empty."
                }

                appendField(
                    builder,
                    key,
                )

                appendField(
                    builder,
                    value,
                )
            }

        return builder.toString()
    }

    fun decodeMetadata(
        payload: String,
    ): Map<String, String> {

        if (payload.isEmpty()) {
            return emptyMap()
        }

        var offset = 0

        val result =
            linkedMapOf<String, String>()

        while (offset < payload.length) {

            val keyResult =
                readField(
                    payload,
                    offset,
                )

            offset =
                keyResult.nextOffset

            val valueResult =
                readField(
                    payload,
                    offset,
                )

            offset =
                valueResult.nextOffset

            result[
                keyResult.value
            ] =
                valueResult.value
        }

        return result
    }

    private fun appendField(
        builder: StringBuilder,
        value: String,
    ) {
        builder
            .append(value.length)
            .append(':')
            .append(value)
    }

    private data class ReadFieldResult(
        val value: String,
        val nextOffset: Int,
    )

    private fun readField(
        payload: String,
        offset: Int,
    ): ReadFieldResult {

        val separator =
            payload.indexOf(
                ':',
                startIndex = offset,
            )

        require(separator >= 0) {
            "Invalid metadata payload."
        }

        val length =
            payload
                .substring(
                    offset,
                    separator,
                )
                .toIntOrNull()
                ?: error(
                    "Invalid metadata field length."
                )

        require(length >= 0) {
            "Metadata field length must not be negative."
        }

        val start =
            separator + 1

        val end =
            start + length

        require(end <= payload.length) {
            "Metadata payload is truncated."
        }

        return ReadFieldResult(
            value =
                payload.substring(
                    start,
                    end,
                ),
            nextOffset = end,
        )
    }
}
