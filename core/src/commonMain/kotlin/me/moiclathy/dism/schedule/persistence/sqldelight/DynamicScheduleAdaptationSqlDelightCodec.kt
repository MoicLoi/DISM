package me.moiclathy.dism.schedule.persistence.sqldelight

import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

import me.moiclathy.dism.schedule.adaptation.persistence
    .DynamicScheduleAdaptationPersistencePayload

/**
 * 14.11
 *
 * Adaptation 记录的 JSON 编解码。
 *
 * `ignoreUnknownKeys = false` 是刻意的：适配记录要能被 14.12 重放，
 * 读到一个"看起来能解析但少了字段"的旧记录，
 * 比起直接报错更难排查。
 */
object DynamicScheduleAdaptationSqlDelightCodec {

    private val json =
        Json {
            encodeDefaults = true
            ignoreUnknownKeys = false
            prettyPrint = false
        }

    fun encode(
        payload:
            DynamicScheduleAdaptationPersistencePayload,
    ): String {
        return json.encodeToString(payload)
    }

    fun decode(
        value: String,
    ): DynamicScheduleAdaptationPersistencePayload {
        return json.decodeFromString(value)
    }
}
