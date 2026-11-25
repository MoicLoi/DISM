package me.moiclathy.dism.schedule.adaptation

/**
 * 14.1
 *
 * 一次决策所能看到的全部历史特征。
 */
data class DynamicScheduleDecisionFeatureSet(
    val features:
        List<DynamicScheduleDecisionFeature> = emptyList(),
) {

    init {
        require(
            features.map { it.key }.distinct().size ==
                features.size,
        ) {
            "feature keys must be unique"
        }
    }

    val size: Int
        get() = features.size

    val isEmpty: Boolean
        get() = features.isEmpty()

    /**
     * 14.2：加 `operator` 之后同一个方法同时支持
     * `features.get(key)` 与 `features[key]` 两种写法。
     */
    operator fun get(
        key: String,
    ): DynamicScheduleDecisionFeature? =
        features.firstOrNull {
            it.key == key
        }

    fun value(
        key: String,
    ): Double? =
        get(key)?.value

    fun confidence(
        key: String,
    ): Double? =
        get(key)?.confidence

    fun contains(
        key: String,
    ): Boolean =
        get(key) != null

    /**
     * 只有来源匹配的特征。
     */
    fun from(
        source: DynamicScheduleDecisionFeatureSource,
    ): List<DynamicScheduleDecisionFeature> =
        features.filter {
            it.source == source
        }

    companion object {

        val EMPTY =
            DynamicScheduleDecisionFeatureSet()
    }
}
