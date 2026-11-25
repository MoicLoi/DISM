# 日历导入（16.21 / 16.24）

## 链路

```text
ICS 文本
   ↓ DismScheduleImport.fromIcs()      公共 API（16.21）
DynamicScheduleModel
   ↓ DismPresenter
DismUiState → Compose
```

在这之前，`DynamicScheduleModel` 只能由调用方**手工构造**
（各平台入口里的 `DemoSchedule` 就是这么来的）。

而 11.x 里其实早就有完整链路：

```text
ICS 文本
   ↓ IcsScheduleParser
ExternalSchedule
   ↓ ExternalScheduleToDynamicScheduleMapper
DynamicScheduleModel
```

只是它整个住在"约定内部"的包里，没有被 Product API 收口。
于是应用要么手工编数据，要么去碰 200 多个 `ExternalScheduleSync*`
类型——都不对。

## 只收口这一条链

11.x 的绝大部分是同步机制：

```text
Journal / 幂等 / 恢复 / 对账 / 重放 / 事务
```

那些**不**在公共 API 里暴露。它们是 Runtime 的内部事务，
不是产品概念。一旦进入冻结的公共 ABI，就变成 1.x 的承诺。

## 错误处理：用户数据错误用结果表示

```kotlin
DismScheduleImport.fromIcs(...): DismScheduleImportResult
```

```text
调用方错误     →  抛异常        例如 sourceId 为空白（domain 里已 require）
用户数据错误   →  返回结果      这个文件解析不了 / 有一条坏了
```

理由不是"哪个更常见"，而是两个具体后果：

```text
1. 抛异常会丢掉 warnings
   一个含 3 条好 VEVENT、1 条坏 VEVENT 的文件
   应该导入 3 条**并告诉用户第 4 条坏了**；
   抛异常会把这些信息一起扔掉。

2. 用户选错文件是常规情况，不是异常情况
   不该迫使调用方为它写 try/catch。
```

这与 `DismBootstrap.createPersistence` 抛异常并不矛盾：
那是"配置要求了平台做不到的事"，属于调用方错误。

## 三端取文件

```text
桌面      AwtCalendarPicker           FileDialog（系统原生）
Android   AndroidCalendarPicker(SAF)  ActivityResultContracts.OpenDocument
Web       WebCalendarPicker          隐藏的 <input type="file">
```

### 为什么是注入的 `CalendarPicker`，不是 expect/actual

三个平台的取文件方式差别很大，而且 Android 与 Web 是
**异步 + 回调**式的，没法用一个同步的 expect 函数表达。

所以共享层只声明"我需要一段 ICS 文本"：

```kotlin
fun interface CalendarPicker {
    suspend fun pickIcs(): String?
}
```

返回 null 表示用户取消——取消是正常操作，不该抛异常也不该报错。

`calendarPicker == null` 的平台**不渲染** Import 按钮。
没接线的平台不显示一个点了没反应的入口。

### 桌面为什么用 AWT 的 `FileDialog`

```text
FileDialog      操作系统原生对话框，观感与系统一致
JFileChooser    Swing 自绘，在 Windows 上明显"不像这个系统"
```

文件选择框是最容易被感知到"外行"的地方之一。

### Android 的 MIME 类型为什么给两个

```text
text/calendar    标准
text/*           有些 provider 把 .ics 标成通用文本
```

只给前者时，部分设备上文件会灰掉选不中——
那是"功能看起来坏了"的典型表现。

### Web 为什么用 `kotlinx-browser` 而不是 `kotlin-browser`

```text
kotlin-browser（wrappers）   只有 js 变体
webMain                      同时被 js 与 wasmJs 编译
kotlinx-browser              两个变体都有
```

## 缓存策略：存原始输入，每次重新解析

```text
桌面      ~/.dism/calendar.ics
Android   filesDir/dism-calendar.ics
Web       localStorage
```

### 为什么是应用在缓存，而不是 DISM 在持久化

DISM 目前**没有**"持久化 schedule model"的公共 API——
落盘的只有决策历史（Snapshot / Trace / ReplayInput）。

两个选择：

```text
A. 加一个 API 让 DISM 存模型    工作量大，且要先设计模型生命周期
B. 应用把用户导入的 ICS 存起来   最小、且语义诚实
```

选 B 的理由不只是省事：

```text
ICS 是**用户的原始输入**，不是 DISM 的状态
```

存原始输入 + 每次启动重新解析，比存一份 DISM 内部模型更不容易
出现"库里那份和代码里的解析规则不一致"：

```text
解析规则升级后  →  重新导入自动得到新结果
存下来的旧模型  →  会一直停留在旧规则上
```

这一点由 `cacheIsReparsedRatherThanStoredAsAModel` 锁住。

### 解析失败不删缓存

```kotlin
// StartupCalendar
return parsed.model ?: fallback()
```

解析规则变严、或文件被手工改坏，都只应该导致"这次退回兜底"，
而不是"用户的日历没了"。

删掉缓存会把"只是这次解析失败"变成"数据永久丢失"。

### 导入结果三态

```kotlin
sealed interface ImportOutcome {
    data object Cancelled                                   // 用户取消
    data class Imported(model, feedback)                    // 有模型可用
    data class Rejected(feedback)                           // 没有可用模型
}
```

三态而不是"成功/失败"，因为对界面意味着不同的反应：

```text
Cancelled   什么都不做（取消不是错误）
Imported    更新模型 + 显示反馈
Rejected    不更新模型 + 显示错误
```

### 缓存失败必须告诉用户

```text
缓存写失败  →  反馈里加一条 "Imported, but could not be saved for next time"
```

吞掉的话界面会显示"导入成功"，而重启后它不见了——
用户会以为是 DISM 弄丢了数据。

这与 `load()` 吞异常是**不对称**的，而这是刻意的：

```text
load 失败  →  界面显示"没导入过"，符合事实
save 失败  →  界面显示"导入成功"，而下次它不见了 ← 骗人
```

### 部分成功要单独说

一个含 3 条好、1 条坏的 ICS 会导入 3 条。只显示"导入成功"
会让用户以为日历是完整的，从而漏掉那一条——而那可能正是他要的。

```text
全部成功    Imported 3 event(s).
部分成功    Imported 3 event(s), but some were skipped. + 错误详情
完全失败    Nothing could be imported. + 错误详情
```

## 验证程度

```text
桌面       ✅ 真实运行验证（含端到端：db 里能查到导入的 item id）
Android    ⚠ 仅编译验证（无设备/模拟器）
Web        ⚠ 仅编译验证（js + wasmJs 都编译通过）
```

Android 的 SAF 对话框是否弹出、Web 的隐藏 input 是否能触发、
wasmJs 下 change 事件的类型桥接是否正确——**都未验证过**。

## 一个 Web 特有的限制

浏览器没有文件系统，SQLDelight 落盘不可用，所以：

```text
刷新页面  →  日历还在（localStorage）
            决策历史也还在（localStorage）   ← 16.34
```

16.34 之前历史在内存里、刷新即清空。现在两者都持久化，
详见 [Web Persistence](web-persistence.md)。

## 仍然存在的一件事

`SampleCalendar` 还在，作为"还没有导入过任何日历"时的兜底。

它不再是"绕过导入链路的手工对象"——它给出的是一段 ICS，
走的是同一个 `DismScheduleImport`。接上设备日历之后应该被替换掉。
