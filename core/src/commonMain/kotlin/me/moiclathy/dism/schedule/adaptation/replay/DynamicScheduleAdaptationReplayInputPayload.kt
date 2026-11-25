package me.moiclathy.dism.schedule.adaptation.replay

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 14.12
 *
 * Replay Input 落盘时的 JSON 形态。
 */
@Serializable
data class DynamicScheduleAdaptationReplayInputPayload(
    val input:
        DynamicScheduleAdaptationReplayInput,

    val schemaVersion: Long,
)

/**
 * 14.12
 *
 * Replay Input 的编解码。
 *
 * `allowStructuredMapKeys = true` 是必需的：
 * [DynamicScheduleAdaptationReplayInput.histories] 的 key
 * 是 attention key 字符串，而 `DynamicScheduleModel` 里
 * 还有其它结构化 key（如 `Set<String>`），
 * 关掉它会让这些对象无法编码。
 *
 * 版本校验失败直接抛异常，而不是"尽量解析"——
 * 拿到一份结构已经变了的输入去做重放，
 * 得到的差异毫无意义。
 */
object DynamicScheduleAdaptationReplayInputPayloadCodec {

    const val VERSION: Long = 1L

    val json =
        Json {
            encodeDefaults = true
            allowStructuredMapKeys = true
            ignoreUnknownKeys = false
            prettyPrint = false
        }

    fun encode(
        input: DynamicScheduleAdaptationReplayInput,
    ): String =
        json.encodeToString(
            DynamicScheduleAdaptationReplayInputPayload.serializer(),
            DynamicScheduleAdaptationReplayInputPayload(
                input = input,
                schemaVersion = VERSION,
            ),
        )

    fun decode(
        payload: String,
    ): DynamicScheduleAdaptationReplayInput {

        val decoded =
            json.decodeFromString<
                DynamicScheduleAdaptationReplayInputPayload
            >(payload)

        require(
            decoded.schemaVersion == VERSION,
        ) {
            "Unsupported adaptation replay input schema: " +
                decoded.schemaVersion
        }

        return decoded.input
    }
}
