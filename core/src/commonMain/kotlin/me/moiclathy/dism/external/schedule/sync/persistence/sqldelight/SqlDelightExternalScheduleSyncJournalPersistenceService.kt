package me.moiclathy.dism.external.schedule.sync.persistence.sqldelight

import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncJournalPersistenceRecord
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncJournalPersistenceRepository

/**
 * 可选的 SQLDelight-specific service facade。
 *
 * 默认推荐继续通过 11.24 的 generic persistence service 调用 repository；
 * 本 facade 仅用于需要明确 infrastructure 类型的 composition root。
 */
class SqlDelightExternalScheduleSyncJournalPersistenceService(
    private val repository: ExternalScheduleSyncJournalPersistenceRepository,
) {
    fun append(record: ExternalScheduleSyncJournalPersistenceRecord) =
        repository.append(record)

    fun appendAll(records: List<ExternalScheduleSyncJournalPersistenceRecord>) =
        repository.appendAll(records)
}
