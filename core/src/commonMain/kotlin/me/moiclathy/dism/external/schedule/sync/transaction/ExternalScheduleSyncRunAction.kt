package me.moiclathy.dism.external.schedule.sync.transaction

import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction

/**
 * Run 中的一项 Action。
 *
 * 11.21 不复制 11.15 的执行记录，
 * 这里只提供事务层 Recovery 所需要的信息。
 */
data class ExternalScheduleSyncRunAction(

    val externalKey:
        String,

    val action:
        ExternalScheduleSyncAction,

    val completed:
        Boolean = false,

    val failed:
        Boolean = false,

    val eventId:
        String? = null,

    val message:
        String? = null,
) {

    init {

        require(externalKey.isNotBlank()) {
            "externalKey must not be blank"
        }

        require(
            !(completed && failed)
        ) {
            "Action cannot be both completed and failed"
        }

        if (failed) {
            require(!message.isNullOrBlank()) {
                "message is required for failed action"
            }
        }
    }
}
