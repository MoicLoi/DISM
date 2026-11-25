# Semantic Model

## Entity

用户世界里真实存在的东西：人、地点、物品、课程、活动、组织。

## Relation

Entity 之间的连接。**不假设永久为真**：

```text
ACTIVE / SUPERSEDED / INVALIDATED
```

## Evidence

解释 DISM 为什么相信某件事。它让系统能区分：

```text
known   /  inferred  /  uncertain
```

## State

当前被认为为真的状态。状态是动态的：

```text
PowerBank located_at Dorm
        ↓
PowerBank located_at Backpack
```

旧状态不会被简单覆盖——它的有效性与证据可以被保留。

## Event

现实中观察到的变化：

```text
Reality → Observation → Event → State transition
```

## 实现位置

```text
me.moiclathy.dism.domain        Entity / Relation / Predicate
me.moiclathy.dism.semantic      Observation / Resolution / Engine
me.moiclathy.dism.state         Reducer / Temporal / Query
me.moiclathy.dism.evidence      Evidence registry
```

这些都是 **内部实现**。应用只通过 `DismRuntimeApi` 与它们交互。
