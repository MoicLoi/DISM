# 决策记录与诊断（16.22 / 16.23 / 16.26）

## 问题：什么时候该把决策记进历史

两个极端都不对：

```text
每次加载都评估并记录    →  打开 100 次 = 100 条一模一样的记录
从不记录（只用 preview）→  History 页永远是空的
```

第二条尤其隐蔽——它曾经真实存在过：

```text
1.1.0  展示层从 evaluate 换成 preview
       修掉了"每次加载都写"（历史无限增长）
       引入了"从不写"（History 页永远为空）
```

单测、门禁、编译全绿，而那个页面是死的。
这类问题只有真的跑起来才会暴露。

## 中间那条线：变了才记

```text
每次加载都写  →  历史无限增长
从不写        →  历史永远为空
变了才写      →  历史 = 决策何时改变的日志     ← 现在
```

### 为什么"变了才记"是正确的问题

History 页要回答的是：

```text
它凭什么这么说？  什么时候变成这样的？
```

所以有价值的历史是**决策变化的日志**，
而不是"这个东西被看过几次"的记录。

### 实现不需要任何新存储

```kotlin
facade.preview(model, now)        这次会是什么结果（不落盘）
facade.latestDecision(itemId)     上次存下来的是什么
两者不同  →  facade.evaluate(model, now) 落盘
```

不引入"已记录指纹"之类的第二份状态，
也就不会出现"指纹与历史不一致"的问题。

### 三态在界面与测试上的体现

```kotlin
DecisionRecorder(dism).recordIfChanged(model, now): Boolean
```

放在 `app/shared` 的展示层，不进 Core：

```text
Core 只提供两个原语    preview 算 / evaluate 算 + 记
"什么时候记"是产品决策
```

### 比对是精确的，不加 epsilon

`priority` 用 `==` 比较，看起来危险（浮点比较通常该留容差）。
但 priority 是**确定性计算**的结果，而且 `preview` 与 `evaluate`
共用同一份投影代码：

```text
同一个输入  →  逐位相同
有差异      →  确实是适配权重或上下文变了
```

而后者**正是应该被记进历史的东西**。加 epsilon 反而会把
"适配把它调整了 0.001"这种真实变化当成没变。

如果哪天真出现浮点噪声导致的重复记录，
正确的修法是消除不确定性，而不是放宽比较。

### 它在真实应用里的表现

```text
首次运行（无记录）    e2e-1 出现  0 →  9    记了 1 条
重启，模型未变        e2e-1 出现  9 →  9    没记
改内容后重启          e2e-1 出现  9 → 18    记了 1 条
```

（`e2e-1` 是 ICS 里的 UID，出现在落盘的 Snapshot / Trace 中。）

## 记录失败：吞掉，但必须报告

`DecisionRecorder` 有意吞掉异常：

```text
记录失败  →  History 少一条
抛出      →  整个 Today 页变成错误页
```

"看不了历史"远好于"看不了日程"。主功能不该被一个附属写入的失败带崩。

### 但吞掉不等于不告诉任何人

```text
吞掉且不报告  →  历史悄悄不长了，查不出来
吞掉并报告    →  页面照常可用，但诊断里有据可查
```

这就是"有意降级"与"静默失败"的区别。

## 三条通道，缺一不可

```text
1. 诊断事件     给开发者
2. 界面提示     给用户
3. 自动重试     让状态能恢复
```

### 1. 诊断事件（16.23）

```text
[DISM WARN] presentation/recordDecision DECISION_RECORD_FAILED:
  Failed to record the decision: [SQLITE_READONLY]
  Attempt to write a readonly database
```

`AppDiagnosticSink` 同时"打印到控制台"和"留在内存里"——
前者让开发时立刻看到，后者让测试能断言。

**不需要 Core 改动**：`DismDiagnosticSink` 是公共的 `fun interface`。
应用自己创建它并传给 `DismConfiguration.diagnostics.sink`，
于是天然持有引用，可以直接往它写。

事件带 `attributes["errorType"]`：不同失败原因（磁盘满 / 权限 /
数据库被锁）在排查时是完全不同的问题，而 message 里往往看不出类型。
测试断言用的是 code（`DECISION_RECORD_FAILED`）而不是 message——
那句话是给人看的、随时会改。

### 2. 界面提示（16.26）

```text
New decisions could not be recorded.
This list may be missing recent entries, so it is not the full story.
[SQLITE_READONLY] Attempt to write a readonly database
```

横幅放在条目**之前**：它改变的是"下面这些还能不能信"，
而不是某一条的细节。

原因**原样显示**，不吞成"未知错误"——用户至少能判断
"是不是磁盘满了 / 权限问题"，而不是只能报一个无法复现的 bug。

### 3. 自动重试

判定"是否要记录"是拿当前决策与**已存下来的**比对。
所以一次失败的写入会让下次加载仍然判定"变了"、于是重试。

```text
成功之后 recordingError 自动变回 null
```

也就是说 `HistoryUiState.recordingError` 反映的是
"**现在**记录是否可用"，不是一个历史累计错误。
如果它粘住不走，用户会以为问题一直在，而实际上早就好了。

## 状态字段

```kotlin
data class HistoryUiState(
    ...
    val recordingError: String? = null,
) {
    val canRecord: Boolean
        get() = recordingError == null

    val historyMayBeIncomplete: Boolean
        get() = recordingError != null
}
```

## 全局可见性：导航角标（16.28）

History 页的横幅只在用户走进那一页时才看得到。
所以在导航项上加一个角标，任何页面都能看到。

```text
Today / Schedule / Attention / … 的导航栏
  History  ⚠
```

### 为什么不是全屏横幅

这是一个**附属功能**的故障：

```text
日程照常可用        它不紧急
但历史在悄悄缺失    它重要且静默
```

全屏横幅一定会被看到，代价是长期占用主界面的注意力。
而角标：

```text
任何页面都能看到    不占内容空间
且直接指向详情所在页
```

这与"强调必须稀缺"是同一条原则——`Attention` 是全页唯一
使用高饱和容器的区域，因为到处都是强调等于没有强调。

### 角标只落在 History 上

记录失败只影响历史，不影响日程本身。
在每个页面上都挂角标会让"有东西需要注意"变成噪声。

### 无障碍

角标不只是一个 `!`：

```kotlin
badgeDescriptionFor(...)
// "History may be incomplete: recent decisions could not
//  be recorded."
```

读屏如果只念"History 感叹号"，用户并不知道发生了什么。

角标与描述**必须同时在或同时不在**——
只有角标没有描述，读屏用户会看到一个解释不了的符号；
反过来则是什么都看不到、却被告知出了问题。
`theDescriptionMatchesTheBadge` 锁住这条。

### 它是纯函数

```kotlin
internal fun badgeFor(
    destination: DismDestination,
    state: DismUiState,
): String?
```

"哪个目的地该出现角标"是一条产品规则，埋在 `when` 里
靠肉眼维护的话，它会随着页面增加而悄悄失准。
抽出来之后可以穷举测试，而 `when` 是穷举的（没有 `else`），
所以新增目的地时编译器会提醒做一次决定。
