# Dynamic Schedule

调度不是"存一条日程"，而是一次决策链：

```text
Model
  ↓ 12.2 State Projection
Projected Model
  ↓ Dependency Resolution
  ↓ Context Binding
  ↓ Attention Memory
  ↓ Priority Adaptation
  ↓ Attention Projection
  ↓ Conflict Resolution
  ↓ Attention Selection
  ↓ Explanation
Decision
  ↓ Persistence Bridge
Snapshot + Trace + ReplayInput
```

## 历史与重放

```text
Snapshot    不可变的历史证据
Trace       这次决策执行了什么
ReplayInput 重放所需的完整输入
Provenance  重放记录（不参与 Retention 级联删除）
```

历史可以重放并验证：

```text
EXACT_MATCH / INPUT_CHANGED / POLICY_CHANGED
ENGINE_CHANGED / BEHAVIOR_CHANGED / VERIFICATION_FAILED
```

## 自适应层（14.x）

```text
14.1 Feature → 14.2 Interpretation → 14.3 Behavior Model
        → 14.4 Weight Delta
        → 14.5 Attention Delta / 14.6 Priority Delta
        → 14.7 Context
        → 14.8 Confidence → 14.9 Boundary → 14.10 Human Override
        → 14.11 Persistence → 14.12 Replay → 14.13 Verification
        → 14.14 Provenance → 14.16 Explanation → 14.17 Audit
```

**用户显式设置永远优先**：`HUMAN_OVERRIDE` 之上的只有用户自己。

## 契约

内部这些细节**不**出现在产品 API；应用拿到的是
`DynamicScheduleFacade`：

```kotlin
schedule.evaluate(model, now)
schedule.latestDecision(itemId)
schedule.history.query(itemId)
```
