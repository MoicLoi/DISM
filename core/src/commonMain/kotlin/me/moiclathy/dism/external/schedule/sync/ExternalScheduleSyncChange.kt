package me.moiclathy.dism.external.schedule.sync

import me.moiclathy.dism.external.schedule.diff.ExternalScheduleDiffRecord

/**
 * 一条 Diff 对应的一条 Sync Action。
 */
data class ExternalScheduleSyncChange(
    val externalKey: String,
    val action: ExternalScheduleSyncAction,
    val diff: ExternalScheduleDiffRecord,
) {

    init {
        require(externalKey.isNotBlank()) {
            "externalKey must not be blank"
        }

        require(
            externalKey == diff.externalKey
        ) {
            "externalKey must match diff.externalKey"
        }
    }
}
