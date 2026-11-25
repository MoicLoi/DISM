package me.moiclathy.dism.schedule.version

/**
 * 13.10.6
 *
 * 默认版本兼容性注册表。
 *
 * 规则：
 *
 * ```text
 * 版本完全相同  → EXACT
 * 命中已注册规则 → 该规则的结果
 * 其它          → UNKNOWN（不能直接 Replay）
 * ```
 *
 * ### 为什么默认是 UNKNOWN 而不是"兼容"
 *
 * 因为"不知道"和"兼容"是两件事。
 * 把未知版本当成兼容，等于让升级后的系统
 * 拿新语义去跑旧数据，然后默默给出一个
 * 看起来正常、实际上无法解释的决策。
 *
 * 反过来，默认 UNKNOWN 的代价只是"需要显式注册一条规则"。
 */
class DefaultDynamicScheduleHistoryVersionCompatibilityRegistry(
    /**
     * 当前引擎版本。
     *
     * 提供 [resolve] 的单参数重载使用，
     * 让调用方不必每次把当前版本再传一遍。
     */
    val currentVersion:
        DynamicScheduleHistoryEngineVersion,

    private val rules:
        List<DynamicScheduleHistoryVersionCompatibilityRule> =
        emptyList(),
) : DynamicScheduleHistoryVersionCompatibilityRegistry {

    constructor(
        currentVersion: String,

        rules:
            List<DynamicScheduleHistoryVersionCompatibilityRule> =
            emptyList(),
    ) : this(
        currentVersion =
            DynamicScheduleHistoryEngineVersion(currentVersion),

        rules = rules,
    )

    /**
     * 用注册表自己记录的当前版本做判断。
     */
    fun resolve(
        historicalVersion:
            DynamicScheduleHistoryEngineVersion,
    ): DynamicScheduleHistoryVersionCompatibilityResult =
        resolve(
            historicalVersion = historicalVersion,
            currentVersion = currentVersion,
        )

    override fun resolve(
        historicalVersion:
            DynamicScheduleHistoryEngineVersion,

        currentVersion:
            DynamicScheduleHistoryEngineVersion,
    ): DynamicScheduleHistoryVersionCompatibilityResult {

        /*
         * 完全一致永远优先于任何规则：
         * 同名版本之间不存在"兼容性"问题。
         */
        if (
            historicalVersion.value ==
            currentVersion.value
        ) {
            return DynamicScheduleHistoryVersionCompatibilityResult
                .exact(historicalVersion)
        }

        val rule =
            rules.firstOrNull {
                it.matches(
                    historicalVersion = historicalVersion,
                    currentVersion = currentVersion,
                )
            }

        if (rule != null) {
            return rule.resolve(
                historicalVersion = historicalVersion,
                currentVersion = currentVersion,
            )
        }

        /*
         * 第一版默认保守：
         *
         * 不知道的版本一律不能直接 Replay。
         */
        return DynamicScheduleHistoryVersionCompatibilityResult(
            historicalVersion = historicalVersion,

            currentVersion = currentVersion,

            compatibility =
                DynamicScheduleHistoryVersionCompatibility.UNKNOWN,

            reason =
                "No explicit compatibility rule is registered for the historical engine version.",
        )
    }
}
