package me.moiclathy.dism.presentation

import kotlin.time.Instant

import me.moiclathy.dism.api.Dism
import me.moiclathy.dism.schedule.api.DynamicScheduleDecisionView
import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/**
 * 16.22：什么时候该把决策**记进历史**。
 *
 * ### 要解决的问题
 *
 * 两个极端都不对：
 *
 * ```text
 * 每次加载都评估并记录  →  打开 100 次 = 100 条一模一样的记录
 * 从不记录（只用 preview）→  History 页永远是空的
 * ```
 *
 * 中间那条线不是"多久记一次"，而是**"变了才记"**。
 *
 * ### 为什么"变了才记"是正确的问题
 *
 * History 页要回答的是：
 *
 * ```text
 * 它凭什么这么说？  什么时候变成这样的？
 * ```
 *
 * 所以有价值的历史是**决策变化的日志**，
 * 而不是"这个东西被看过几次"的记录。
 * 记录"看过一次"对用户没有任何信息量。
 *
 * ### 它不需要任何新存储
 *
 * 比对是拿"现在算出来的"和"上次存下来的"比：
 *
 * ```text
 * facade.preview(model, now)          这次会是什么结果（不落盘）
 * facade.latestDecision(itemId)       上次存下来的是什么
 * ```
 *
 * 两者不同 → 调 `evaluate` 落盘。
 * 所以不引入"已记录指纹"之类的第二份状态，
 * 也就不会出现"指纹与历史不一致"的问题。
 *
 * ### 它是策略，不是机制
 *
 * `:core` 只提供两个原语：
 *
 * ```text
 * preview   算
 * evaluate  算 + 记
 * ```
 *
 * "什么时候记"是产品决策，所以住在展示层。
 * 如果将来多个宿主需要同一策略，再上移到 Core。
 */
class DecisionRecorder(
    private val dism: Dism,
) {

    /**
     * 决策发生变化时记录一次。
     *
     * @return 是否真的记录了
     */
    suspend fun recordIfChanged(
        model: DynamicScheduleModel,
        now: Instant,
    ): Boolean {

        val schedule = dism.runtime.dynamicSchedule()

        val previewed = schedule.preview(model = model, now = now)

        /*
         * 空模型不记录。
         *
         * 一个没有任何条目的 schedule 写进历史只会让 History 页
         * 多出一条点进去什么都没有的记录。
         */
        if (previewed.decisions.isEmpty()) {
            return false
        }

        /*
         * 16.32：**一次**批量查询，而不是每个条目一次。
         *
         * 这一行之前是：
         *
         * ```kotlin
         * previewed.decisions.any { schedule.latestDecision(it.itemId) ... }
         * ```
         *
         * 也就是 N+1。实测（真实 SQLDelight）：
         *
         * ```text
         * 200 次单条查询      81 ms
         * 一次完整评估         1 ms
         * ```
         *
         * 逐条查询比它守护的那条评估链贵约 80 倍，
         * 而且每次启动都会跑。
         */
        val recordedByItem =
            schedule.latestDecisions(
                previewed.decisions.map { it.itemId },
            )

        val changed =
            previewed.decisions.any { current ->
                val recorded =
                    recordedByItem[current.itemId]

                /*
                 * 从来没有记录过 → 必须记。
                 */
                recorded == null ||
                    !sameDecision(recorded, current)
            }

        if (!changed) {
            return false
        }

        schedule.evaluate(model = model, now = now)

        return true
    }
}

/**
 * 两条决策在**用户可感知的层面**是否相同。
 *
 * 比对的是全部四个字段：
 *
 * ```text
 * title              用户看见的文字变了
 * priority           决定排序与强弱
 * attentionRequired  决定要不要提醒
 * explanation        "凭什么"的原文
 * ```
 *
 * ### priority 用精确比较，不加 epsilon
 *
 * 这看起来危险——浮点比较通常该留容差。
 * 但这里的 priority 是**确定性计算**的结果，
 * 而且两条路径（`preview` 与 `evaluate`）共用同一份投影代码
 * （见 `DefaultDynamicScheduleFacade.toDynamicScheduleEvaluation`）。
 *
 * 所以：
 *
 * ```text
 * 同一个输入  →  逐位相同
 * 有差异      →  确实是适配权重或上下文变了
 * ```
 *
 * 而后者**正是应该被记进历史的东西**。
 * 加 epsilon 反而会把"适配把它调整了 0.001"这种真实变化当成没变。
 *
 * 如果哪天真出现浮点噪声导致的重复记录，
 * 正确的修法是消除不确定性，而不是在这里放宽比较。
 */
private fun sameDecision(
    recorded: DynamicScheduleDecisionView,
    current: DynamicScheduleDecisionView,
): Boolean =
    recorded.itemId == current.itemId &&
        recorded.title == current.title &&
        recorded.priority == current.priority &&
        recorded.attentionRequired == current.attentionRequired &&
        recorded.explanation == current.explanation
