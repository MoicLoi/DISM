package me.moiclathy.dism.schedule.persistence.sqldelight

import kotlinx.serialization.Serializable

import me.moiclathy.dism.schedule.provenance.DynamicScheduleDecisionProvenance

/**
 * 13.16.13
 *
 * Provenance 的 diagnostics 编解码。
 *
 * 用项目的 kotlinx.serialization 配置，而不是手写
 * `["a","b"]` 解析器——手写解析器在遇到引号、换行、
 * 非 ASCII 时就会悄悄出错。
 *
 * 包装成对象而不是裸数组，是为了给未来加字段留位置
 * （裸数组一旦要加字段就得改格式）。
 */
@Serializable
private data class ProvenanceDiagnosticsPayload(
    val values: List<String> = emptyList(),
)

internal object DynamicScheduleReplayProvenanceCodec {

    fun encode(
        values: List<String>,
    ): String =
        ScheduleDecisionPersistenceJson.instance
            .encodeToString(
                ProvenanceDiagnosticsPayload.serializer(),
                ProvenanceDiagnosticsPayload(values),
            )

    fun decode(
        value: String?,
    ): List<String> =
        value
            ?.let {
                ScheduleDecisionPersistenceJson.instance
                    .decodeFromString(
                        ProvenanceDiagnosticsPayload
                            .serializer(),
                        it,
                    )
            }
            ?.values
            ?: emptyList()
}
