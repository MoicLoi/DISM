# 五个页面（16.4 – 16.9）

## 一览

```text
页面              目的地              平台      一句话
──────────────────────────────────────────────────────────────
Today            TODAY               全平台    我几点要干什么
Schedule         SCHEDULE            全平台    我的安排有哪些
Attention        ATTENTION           全平台    凭什么提醒我
Semantic State   SEMANTIC_STATE      桌面/Web  DISM 认为世界是什么样
History          HISTORY             桌面/Web  决策何时改变过
Settings         SETTINGS            全平台    这个构建里有什么
```

Semantic State 与 History 是**钻取型**视图：条目多、密度高、
需要横向扫。参考文档明确说这些不需要在手机上全部显示。

## Today 与 Schedule 的差异是刻意的

```text
Today     过滤终态（已完成 / 已取消 / 已过期），做减法
Schedule  保留终态，做加法
```

这不是不一致，而是回答不同的问题：

```text
Today     我现在该做什么？        → 已完成的不该占位置
Schedule  那件事我确实做完了吗？  → 藏起来会让人以为丢了
```

Schedule 里终态条目用删除线 + 淡化表示，而不是隐藏。
`scheduleKeepsTerminalItemsButTodayDoesNot` 锁住这条差异。

### 未定时的条目单独成组

Schedule 把 `Unscheduled` / `Deadline` 归到"Without a fixed time"一节。

混在时间轴末尾的话，它们会变成一堆"没有时间"的条目的堆叠，
看起来像渲染坏了。单独一个分组标题能让"这些确实没有时间"变得明确。

## 排序：时间轴按时间，不按优先级

```text
09:00  实验动物学   priority 0.2
11:30  午餐         priority 0.5
14:00  项目开发     priority 0.9
```

按优先级排的话 14:00 会跳到 09:00 前面，**时间轴就不再是时间轴**。

优先级只影响每条的表现（高亮强弱），不改变顺序。
没有时间的条目排最后。

## Attention 页：只过滤决策的输出

Attention 页只保留 `attentionRequired == true`，也就是**决策的输出**。

```text
在 UI 里再写一遍"优先级 > 0.7 就该提醒"  →  两套注意力规则
                                             不一致时没人知道该信谁
```

`attentionMatchesDecisionOutputExactly` 断言的是展示层与决策**一致**，
而不是"必须有某条"——什么时候该提醒是 Core 的决定。

### "没有解释"要如实说

决策可能判定需要关注却没给出 `explanation`。那种情况下这一页显示：

```text
No explanation was given for this reminder.
```

而不是"需要你的注意"。后者读起来更舒服，但会把
`AttentionUiState.unexplainedCount` 永久压成 0——
于是"有多少提醒无法解释"这个可观测指标就再也测不出来了。

## Semantic State 页：计数与关系的差别

顶部四个计数：

```text
Entities / Evidence / Transitions
+ active / tracked relations
```

`activeRelationCount` 与 `relationCount` 分开显示：

```text
relationCount        DISM 记得多少
activeRelationCount  DISM 现在相信多少
```

两者之差正是 Semantic State 与普通数据库表的区别。

### 关系目前用 Uuid 显示

```text
5f3a1c2b  located_at  9c21…
```

这不好看，但它是**诚实的**：Entity 名字需要 11.7 Entity Binding
或真实观察数据才能解析，而演示数据里没有。

替代做法是编一个 `"Entity 5f3a"` 之类的标签，但那是假装我们认得它。
契约上 `RelationUi.subjectLabel` 已经是"标签"而不是"id"，
接上名字时 UI 一行不用改。

## History 页：按 item 查询

公共 Facade 的形状就是：

```kotlin
history.query(itemId, DynamicScheduleHistoryQuery(limit))
history.latest(itemId)
```

**没有**"查全部历史"。这不是遗漏：全局历史查询会随历史增长
无上限变慢。所以这一页是"选一条 → 看它的历史"，而不是一张大表。

### 截断必须说出来

`entries.size < totalMatched` 时明确显示 "Showing N of M"。
否则用户会以为一共就只有 N 条。

## Settings 页：只读，这是刻意的

可写的设置项需要真正影响下次装配。而配置是在
`DismFactory.create()` 时一次性生效的：

```text
运行时改一个开关  →  当前 Dism 实例不会变
                  →  那就是"什么都不做的开关"
```

与其放一堆改了没反应的开关，不如如实展示"这个构建里有什么"。
等做出配置持久化与重启装配之后，这里才会有可编辑项。

每一项都注明**它意味着什么能力**，而不是把字段名翻译一遍
（"Dynamic Schedule" → "动态调度" 对用户没有信息量）。

## 空状态

每个页面都有空状态，且措辞要说明**为什么空**：

```text
Today       Nothing scheduled.
Schedule    Nothing here yet.
Attention   Nothing needs your attention.
            DISM only surfaces things that are worth interrupting you for.
```

最后一条尤其重要：Attention 空是**好消息**，不该看起来像故障。
