# Schedule Facade

```kotlin
interface DynamicScheduleFacade {
    val history: DynamicScheduleHistoryFacade

    suspend fun evaluate(model, now): DynamicScheduleEvaluation
    suspend fun latestDecision(itemId): DynamicScheduleDecisionView?
}
```

## 为什么它不是 Session

内部的 `DynamicScheduleSession` 挂载了 History / Replay /
Integrity / Repair / Timeline / Feature / Interpretation /
Behavior / Weight / Attention / Priority / Context / Confidence /
Boundary / Override / Adaptation Persistence / Replay /
Verification / Provenance / Explanation / Audit。

它是一个很好的**内部集成会话**，但不是适合冻结的公共 API。

```text
Product   DynamicScheduleFacade
              ▲
              │  唯一的桥
Internal  DynamicScheduleSession
```

## 结果类型

```kotlin
data class DynamicScheduleEvaluation(
    val evaluatedAt: Instant,
    val decisions: List<DynamicScheduleDecisionView>,
    val diagnostics: List<String>,
)

data class DynamicScheduleDecisionView(
    val itemId: DynamicScheduleItemId,
    val title: String,
    val priority: Double,
    val attentionRequired: Boolean?,   // null = 快照没给出新评估
    val explanation: String?,
)
```

`attentionRequired == null` 不是 false，见
[DynamicScheduleDecisionView 的语义](#)。

## 历史

```kotlin
schedule.history.query(itemId, DynamicScheduleHistoryQuery(limit = 20))
schedule.history.latest(itemId)
```

内部 Snapshot / Trace / Replay / Provenance 对象不会跨过这条边界。
