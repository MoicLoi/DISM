# Persistence

## 端口与实现

```text
DismPersistence            端口容器（commonMain）
      │
      ├── InMemory 实现      commonMain，测试用
      └── SQLDelight 实现    schedule/persistence/sqldelight
```

`DismPersistenceFactory` 负责把平台 driver 装成 SQLDelight 实现。

## 表结构

```text
schedule_decision_snapshot        历史决策证据
schedule_decision_trace           执行轨迹
schedule_decision_replay_input    重放输入
schedule_decision_provenance      重放记录
schedule_adaptation               当前有效适配状态
schedule_adaptation_replay_input  适配重放输入
```

## 谁会被删除

```text
Retention 级联删除：Snapshot → Trace / ReplayInput
不参与级联删除：    Provenance
```

Provenance 往往是旧 Snapshot 被压缩后唯一还能说明
"这里曾经发生过什么"的东西。

## 边界

`DismPersistence` 是 **internal infrastructure**：

```text
应用  →  DismFactory.create()
平台  →  DismPersistenceFactory + DismBootstrapper
```

应用永远不需要知道 SQLDelight 存在。
