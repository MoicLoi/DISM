# 性能（16.17 / 16.31）

## 为什么这里几乎没有计时断言

性能测试最容易写成这样：

```kotlin
assertTrue(elapsed < 500)   // ← 在 CI 上必然不稳
```

机器负载、JIT 预热、GC 都会让同一个操作在 50ms 与 800ms
之间跳。结果是"偶尔失败"，而偶尔失败的测试很快会被所有人
忽略——**包括真正的回归**。

所以 DISM 测的是**调用次数与数据量**：

```text
读几张表？每张读几次？返回多少条？
```

它们是确定的、可复现的，而且是"代价与什么成正比"这个结论的
真正依据。计时只在人工诊断时用。

## 发现：`load()` 每次读四张全表

`DismRuntime.snapshot()` 的实现是：

```kotlin
RuntimeSnapshot(
    entities = persistence.entities.all(),
    evidence = persistence.evidence.all(),
    relationStates = persistence.relationStates.loadAll(),
    transitions = persistence.transitionLog.all(),
)
```

四个**全表读**，返回**全部数据**。

而 `DismPresenter.load()` 每次启动、每次模型变化都调它——
只为了渲染：

```text
四个计数            页面顶部的一行
一张关系列表        Semantic State 页
```

**而那一页是 `desktopOnly` 的——手机端根本打不开，却照样付这份钱。**

### 测量（`RuntimeSnapshotCostTest`，core jvmTest）

用**计数装饰器**观察仓库调用，而不是计时：

```kotlin
private class CountingEntityRepository(
    private val delegate: EntityRepository,
) : EntityRepository by delegate {
    var allCalls = 0; private set
    override fun all(): List<Entity> { allCalls++; return delegate.all() }
}
```

（用 Kotlin 委托实现，只需覆盖关心的那一个方法——
接口将来加方法也不会让测试失效。）

四条断言：

```text
一次 snapshot 读实体表恰好 1 次
snapshot 返回**全部**实体（2000 条进 → 2000 条出）
代价与库大小成正比，与"用了多少"无关
四个集合都存在且一致
```

结论：**代价只取决于库有多大，不取决于用户在看什么。**

## 修法：让代价跟着"用户要看什么"走

```text
之前
  DismPresenter.load()
       └── snapshot()          四张全表读      ← 每次启动都付

之后
  DismPresenter.load()          日程相关，与条目数成正比
  SemanticStatePresenter.load()  库级，与库的大小成正比
       └── 只在用户选中那一页时才调用
```

`App` 里的触发条件：

```kotlin
LaunchedEffect(dism, selected) {
    if (selected == SEMANTIC_STATE && !semanticState.isLoaded) {
        semanticState = withContext(Dispatchers.Default) {
            SemanticStatePresenter(dism).load()
        }
    }
}
```

键刻意**不含** `model`：这一页显示的是库的状态，与日程模型无关，
导入新日历不该让它重读全表。

## 修这个 bug 时引入的新问题：三种状态而不是两种

按需加载之后，"这一页是空的"有了两种截然不同的原因：

```text
isLoaded = false       还没读过          →  "Reading the library state…"
isLoaded = true，0 条    读过了，确实是空的 →  "DISM does not know anything yet"
isLoaded = true，n 条    读过了，有内容
```

**只看计数无法区分前两种。** 把它们合并的话，用户打开这一页会先
看到"DISM 什么都还不知道"——而那是还没去读，不是事实。

```
把一个尚未发生的读取显示成事实，就是撒谎。
```

所以 `SemanticStateUiState` 增加了 `isLoaded`，页面据此显示
读取中还是空。`SemanticStatePresenterTest` 中的
`anEmptyLibraryIsLoadedButEmpty` 专门锁住
"`isLoaded=true` 且 `isEmpty=true`"这一组合。

## 结构性保证：编译器帮你守住边界

`DismUiState` 里**已经没有了** `semanticState` 字段。

这本身是最强的守卫：任何想从 `load()` 的结果里读库级状态的代码
**根本无法编译**。不需要写一条脆弱的"检查源码里没有 snapshot"
的测试——类型系统已经保证了它。

> 能靠类型表达的约束，不要靠测试表达。

## 仍然跑在热路径上的东西（16.32 已修）

```text
DecisionRecorder.recordIfChanged
    └── 曾经：每个 item 一次 latestDecision(itemId)      ← N+1
        现在：一次 latestDecisions(itemIds)              ← 批量
```

### 我差点把它判成"不值得修"

底层只有单条查询 `selectLatestForItem`（走
`(item_id, captured_at_epoch_ms)` 索引）。要消除 N+1 需要一个
批量入口，也就是跨 6 层的公共 ABI 变更。

在动这些之前我写了一份推理，结论是：

```text
它是 N 次带索引的单行查询
而 snapshot() 那次是四张全表的完整物化
→ 两者不是一个量级，可能不值得改 ABI
```

**然后我去量了它。测量的结论正好相反。**

### 实测（`LatestDecisionQueryCostTest`，真实 SQLDelight）

```text
items=200   preview=1ms   n-lookups=85ms
```

**200 次单条查询要 85ms，而整条决策链只要 1ms。**
这个 N+1 比它守护的评估**贵约 85 倍**，而且每次启动都会跑。

修完：

```text
items=200   preview=1ms   n-lookups=85ms   batch=2ms
```

**85ms → 2ms**，与评估本身同量级。

### 为什么这个测试测的是比值

绝对值在不同机器上会变，但"逐条 vs 批量"的比值不会：

```text
机器快慢会同时影响两边
```

而且这条测试**同时断言正确性**：

```kotlin
assertEquals(single, batch[item.id])   // 整份 view 相等
```

性能改进只有在语义不变时才成立。只比某个字段不行——
将来给 view 加字段时，那条断言会静默失去覆盖。

### 跨 6 层的改动

```text
SQL            selectLatestForItems（一条相关子查询）
Store 端口      findLatestByItemIds
                InMemory 与 SQLDelight 两个实现都要写
Snapshot 服务   latestForItems
Session        latestDecisions
Facade          latestDecisions（公共 API）
展示层          DecisionRecorder 改用它
```

### 契约：没记录过的条目不出现

```kotlin
latestDecisions(itemIds): Map<ItemId, DecisionView>
```

不在 map 里的 itemId 表示**从未记录过**——那是调用方
需要区别对待的情况（应当记录一次）。这一点写进了公共 API 的 KDoc。

### 踩到的 KMP 陷阱

第一版在 commonMain 里用了 `putIfAbsent`：

```text
它在 JVM 上能编译
  （kotlin.collections.LinkedHashMap 在 JVM 上就是
    java.util.LinkedHashMap，那个方法来自 java.util.Map）
但它**不是** Kotlin common 的成员
```

`:core:compileKotlinJvm` 通过，而 `checkKotlinAbi` 触发的
iOS 编译失败才暴露出来。

```text
教训：写 common 代码时，"在 JVM 上顺理成章"的方法
      要先确认它在 common 里存在
      （本项目此前已因 toSortedSet 吃过一次同类亏）
```

## 还没做的

```text
启动耗时            需要真机测量
重组次数            需要 Compose 测试基建
内存占用            需要真机 profiler
滚动性能            需要真实长列表
```

这些都是"需要设备或人工"的一类，与 Android 侧
"仅编译验证"的限制相同。
