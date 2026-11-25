package me.moiclathy.dism.external.schedule.normalization

/**
 * External Schedule 规范化策略。
 *
 * 规范化只处理“表示形式”，
 * 不改变外部日程的实际语义。
 */
data class ExternalScheduleNormalizationPolicy(
    val trimText: Boolean = true,
    val collapseWhitespace: Boolean = true,
    val emptyTextToNull: Boolean = true,
    val normalizeCourseCode: Boolean = true,
    val normalizeLocation: Boolean = true,
    val preserveOriginalMetadata: Boolean = true,
) {

    init {
        require(
            trimText || collapseWhitespace
        ) {
            "At least one text normalization strategy must be enabled."
        }
    }
}
