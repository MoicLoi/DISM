# Getting Started

## 最小可用

```kotlin
val dism = DismFactory.create()
```

默认是**内存**存储，任何平台都能跑，也不会假装已经落盘。

## 带上配置

```kotlin
val dism =
    DismFactory.create(
        DismConfiguration(
            runtime = DismRuntimeConfiguration(
                enableAttention = true,
                enableDynamicSchedule = true,
                enableAdaptation = true,
            ),
        ),
    )
```

## 读状态

```kotlin
val snapshot = dism.runtime.snapshot()

snapshot.entities
snapshot.relationStates

dism.runtime.currentStates()
```

## 跑调度

```kotlin
val schedule = dism.runtime.dynamicSchedule()

val evaluation = schedule.evaluate(model, now)

evaluation.decisions          // 每个 item 一个 view
evaluation.attentionRequiredCount

schedule.latestDecision(itemId)
schedule.history.query(itemId, DynamicScheduleHistoryQuery(limit = 20))
```

## 需要真实落盘时

`DismStorageMode.LOCAL` 需要平台层绑定 `DismPersistenceProvider`：

```kotlin
val dism =
    DismBootstrapper(
        persistenceProvider = JvmPersistenceProvider(dbFile),
    ).create(
        DismConfiguration(
            storage = DismStorageConfiguration(
                mode = DismStorageMode.LOCAL,
                databaseName = "dism",
            ),
        ),
    )
```

**没有绑定就配置 LOCAL 会立刻报错**，而不是悄悄退化成内存存储。
