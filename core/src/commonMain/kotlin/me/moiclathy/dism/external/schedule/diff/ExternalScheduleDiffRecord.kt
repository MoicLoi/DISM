package me.moiclathy.dism.external.schedule.diff

import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord

/**
 * 单条外部课程记录的 Diff。
 */
data class ExternalScheduleDiffRecord(
    val externalKey: String,

    val type: ExternalScheduleDiffType,

    val previous:
        ExternalSchedulePersistenceRecord?,

    val current:
        ExternalSchedulePersistenceRecord?,
) {

    init {
        require(externalKey.isNotBlank()) {
            "externalKey must not be blank"
        }

        when (type) {

            ExternalScheduleDiffType.ADDED -> {
                require(current != null) {
                    "ADDED record requires current record"
                }

                require(previous == null) {
                    "ADDED record must not have previous record"
                }
            }

            ExternalScheduleDiffType.CHANGED -> {
                require(previous != null) {
                    "CHANGED record requires previous record"
                }

                require(current != null) {
                    "CHANGED record requires current record"
                }
            }

            ExternalScheduleDiffType.UNCHANGED -> {
                require(previous != null) {
                    "UNCHANGED record requires previous record"
                }

                require(current != null) {
                    "UNCHANGED record requires current record"
                }
            }

            ExternalScheduleDiffType.REMOVED -> {
                require(previous != null) {
                    "REMOVED record requires previous record"
                }

                require(current == null) {
                    "REMOVED record must not have current record"
                }
            }
        }
    }
}
