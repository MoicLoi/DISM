# Attention

Attention 回答的是"**现在该不该打扰用户**"。

## 投影与冲突

```text
AttentionProjection  候选与建议
AttentionConflict     冲突与硬约束
AttentionSelection    最终选择
AttentionExplanation  为什么
```

## 为什么不等于优先级

高优先级 ≠ 该打断用户。所以 Attention 与 Schedule Priority
是两条独立的信号，由 Conflict Resolution 合并。

## 自适应对 Attention 的影响（14.5）

第一版只让两个信号直接影响 Attention：

```text
recentActivity   最近确实在动这件事
postponement     确实被反复推迟
```

`completion` / `cancellation` 的敏感度默认是 **0**：

```text
"经常完成" 不代表应该更频繁打扰用户
"取消"     可能意味着不需要，也可能意味着冲突，方向不唯一
```

## 配置

```kotlin
DismConfiguration(
    runtime = DismRuntimeConfiguration(enableAttention = false),
)
```

关闭后 `dism.capabilities.attention` 会如实变成 `false`。
