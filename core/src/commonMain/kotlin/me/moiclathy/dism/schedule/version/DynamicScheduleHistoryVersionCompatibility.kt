package me.moiclathy.dism.schedule.version

/**
 * 13.10
 *
 * 历史版本与当前引擎的兼容性。
 *
 * 刻意**不叫** `SUPPORTED`——那个词太模糊。
 * [REPLAY_COMPATIBLE] 明确说明：
 *
 * > 它支持的是**历史 Replay**，
 * > 而不是说整个系统所有行为都兼容。
 *
 * 例如 13.1 → 13.2 可能只是内部实现升级，
 * 没有改变 Replay 所依赖的数据语义；
 * 而 13.x → 14.x 可能改了 Priority 计算公式。
 * 两者完全不是一回事，所以不能用"版本号相近"来判断。
 */
enum class DynamicScheduleHistoryVersionCompatibility {

    /**
     * 历史版本与当前 Runtime 完全一致。
     */
    EXACT,

    /**
     * 版本不同，但历史 ReplayInput 可以直接交给当前 Replay Engine。
     */
    REPLAY_COMPATIBLE,

    /**
     * 不能直接 Replay，但存在明确的历史数据迁移路径。
     */
    MIGRATABLE,

    /**
     * 版本不同，并且没有已知安全迁移路径。
     */
    INCOMPATIBLE,

    /**
     * 历史版本格式未知（没有注册任何规则）。
     *
     * 这是默认结果：**不知道就不能直接重放**。
     */
    UNKNOWN,
}
