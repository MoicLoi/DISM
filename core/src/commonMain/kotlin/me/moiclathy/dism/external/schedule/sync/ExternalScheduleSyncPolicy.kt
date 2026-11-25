package me.moiclathy.dism.external.schedule.sync

import me.moiclathy.dism.external.schedule.diff.ExternalScheduleDiffPolicy

/**
 * Schedule Sync 策略。
 */
data class ExternalScheduleSyncPolicy(

    /**
     * Diff 使用的比较策略。
     */
    val diffPolicy:
        ExternalScheduleDiffPolicy =
            ExternalScheduleDiffPolicy(),

    /**
     * 是否对 unchanged 记录生成 NO_OP。
     *
     * 默认保留，方便审计和 E2E。
     */
    val includeNoOp: Boolean = true,

    /**
     * 是否允许创建 Event。
     */
    val allowCreate: Boolean = true,

    /**
     * 是否允许更新 Event。
     */
    val allowUpdate: Boolean = true,

    /**
     * 是否允许处理 removed Event。
     */
    val allowRemovalHandling: Boolean = true,
)
