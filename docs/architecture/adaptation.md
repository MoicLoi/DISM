# Adaptation

Adaptation 让历史行为影响未来的决策权重——但**有明确的边界**。

## 三个必须分开的概念

```text
Influence     系统应该改变多少
Confidence    系统有多确定这种改变是合理的
Boundary      即使允许调整，最多允许调整多少
```

把三者混在一起会产生最危险的一类 bug：
"系统不确定"被当成"任务不重要"。

## 安全边界（14.9）

```text
单次最多 ±0.10
累计最多 ±0.20
24h 内最多 3 次
用户显式设置时完全不介入
```

## 历史行为的方向问题

推迟历史**不**直接等于"少提醒"。14.5 第一版把它当作正向
Attention 信号：

```text
经常推迟 → 未来决策时值得重新关注
```

真正的 increase / reduce 由 Context 决定。

## 可追溯

```text
14.14 Provenance   结构化因果图（机器可读）
14.16 Explanation  一句话 + 决定性因素（人可读）
14.17 Audit        这个决策在生命周期里发生了什么
```

Audit 的核心价值是 `SUPERSEDED`：

```text
Decision A（0.50）
    ↓ 新的 Evidence / Context / Memory
Decision B（0.70）
    ↓
A 被 B supersede
```

被取代**不是**删除。否则"为什么昨天这么排、今天变了"永远答不出来。
