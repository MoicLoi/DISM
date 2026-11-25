package me.moiclathy.dism.external.schedule.normalization

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalSchedule

/**
 * External Schedule 规范化器。
 */
interface ExternalScheduleNormalizer {

    /**
     * 将原始 ExternalSchedule 转换成规范化模型。
     */
    fun normalize(
        schedule: ExternalSchedule,
        normalizedAt: Instant,
    ): ExternalScheduleNormalizationResult
}
