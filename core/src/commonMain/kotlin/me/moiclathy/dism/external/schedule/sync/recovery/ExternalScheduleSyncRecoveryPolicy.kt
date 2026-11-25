package me.moiclathy.dism.external.schedule.sync.recovery

/**
 * Recovery 策略。
 */
data class ExternalScheduleSyncRecoveryPolicy(

    /**
     * 是否重试失败的 CREATE。
     */
    val retryFailedCreate: Boolean = true,

    /**
     * 是否重试失败的 UPDATE。
     */
    val retryFailedUpdate: Boolean = true,

    /**
     * 是否重试失败的 REMOVAL。
     */
    val retryFailedRemoval: Boolean = true,

    /**
     * Event mapping 丢失时是否允许重新创建。
     */
    val recreateMissingMapping: Boolean = true,

    /**
     * 是否对 active + 无 Event ID 的当前记录进行 Reconcile。
     */
    val reconcileMissingMapping: Boolean = true,

    /**
     * 是否跳过已经成功的同步。
     */
    val skipSuccessful: Boolean = true,
)
