# Architecture Overview

DISM 最核心的一条链，其它一切都在服务它：

```text
Entity ── Relation ── Evidence
   │
   └── State ── Event
              │
              ▼
       Dynamic Schedule
              │
              ▼
          Attention
              │
              ▼
          Adaptation
```

## 四层边界（15.x 冻结）

```text
┌────────────────────────────────────┐
│           Product API              │
│  Dism / DismFactory                │
│  DismRuntimeApi                    │
│  DynamicScheduleFacade             │
├────────────────────────────────────┤
│        Domain / Runtime            │
│  Semantic / State / Event          │
│  Schedule / Attention / Adaptation │
├────────────────────────────────────┤
│      Integration / Adapter         │
│  External Schedule / ICS / Sync    │
├────────────────────────────────────┤
│        Infrastructure              │
│  Persistence / SQLDelight / Driver │
└────────────────────────────────────┘
```

依赖方向**只能向下**。上层可以引用下层，下层不得引用上层。

## 为什么这么分

DISM 的问题不是"功能不够"，而是"内部能力远多于公共 API"。
因此 15.x 的目标是把**产品边界**做完，而不是继续堆功能。
