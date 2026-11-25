# Runtime API

```kotlin
interface DismRuntimeApi {
    fun observe(draft, context): ObserveResult
    fun observeAndEvaluate(draft, context, userId, attentionPolicy): ReactiveEvaluationResult
    fun currentStates(): List<RelationState>
    fun queryAt(instant, subjectId, predicate, objectId, minimumConfidence): List<RelationState>
    fun snapshot(): RuntimeSnapshot
    fun dynamicSchedule(identityByItem): DynamicScheduleFacade
}
```

## observe vs observeAndEvaluate

```text
observe              只维护世界状态（纯入口）
observeAndEvaluate   维护状态的同时跑传播 → 建议 → 注意力
```

## snapshot 的语义

`snapshot()` 每次**实时从持久化层读取**，反映 observe 之后的当前状态。

它不是"昨天那次的快照"——历史查询请用调度侧的历史 API。

## 已知不足

一个只想调 `snapshot()` 的应用，也必须实现（或面对）全部六个方法。
15.12 的 Example Application 把这个问题暴露了出来：

```text
一个 Example Application
         ↓
被强迫依赖整个 Runtime Contract
```

后续应把 `DismRuntimeApi` 拆成 runtime / schedule / state /
diagnostics / providers 几块，而不是继续往一个接口里塞。
