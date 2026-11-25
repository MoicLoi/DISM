# UI 架构（16.1 – 16.3 / 16.25）

## 分层

```text
Core Domain
     ↓
Public API / Facade          Dism / DismRuntimeApi / DynamicScheduleFacade
     ↓
Presentation Model           presentation/   纯 Kotlin，可跨端测试
     ↓
Compose UI                   ui/            只认识 UiState
     ↓
Platform Adaptation          desktopApp / androidApp / webApp
```

依赖方向**只能向下**。

```text
UI 不认识 Core 模型
Platform 不认识 Core 内部类型
Presentation 不认识 Compose
```

最后一条是这个结构里最容易被破坏的：`presentation/` 里的文件
**没有** `@Composable`，所以它能在普通单测里被验证，
不需要 Compose 测试环境。

## 一次评估，三个页面共享

UI 里有三个页面都要看日程：

```text
Today      今天该做什么
Schedule   我的安排有哪些
Attention  凭什么提醒我
```

它们**共用一次评估**：

```text
DismPresenter.load()
     ↓  facade.preview(model, now)   评估一次
     ├── HomeProjection       纯函数
     ├── ScheduleProjection   纯函数
     └── AttentionProjection  纯函数
     ↓
DismUiState → Compose
```

### 为什么不各评估一次

`facade.evaluate()` / `preview()` 要跑完整的

```text
Dependency → Context → Attention Memory → Adaptation
    → Attention Projection → Conflict → Explanation
```

链路。如果三个页面各跑一次：

```text
1. 同一个模型被算三遍（切换 tab 尤其明显）
2. 三次之间若有状态变化，界面会出现互相矛盾的视图
```

第二条比第一条严重：用户会看到 Today 说"要准备"而 Attention 里没有它。

## 投影是纯函数

```kotlin
fun homeUiState(
    model: DynamicScheduleModel,
    evaluation: DynamicScheduleEvaluation,
    now: Instant,
    timeZone: TimeZone,
): HomeUiState
```

不读时钟、不持有状态。`now` 与时区都由调用方传入
（与 13.15 的时钟纪律一致），所以同一份输入永远得到同一份 UiState。

时区**必须**显式可传：否则 UiState 会随运行机器变化，
单测就只能断言"包含一个冒号"这类没意义的东西。

## 导航：按宽度，不按平台

```text
窄  (maxWidth < 600dp)   底部导航栏，只放 primary 目的地
宽  (maxWidth >= 600dp)  左侧导轨，primary + desktopOnly
```

用 `BoxWithConstraints` 判断**可用宽度**，而不是判断平台。
好处有两个：

```text
1. 桌面窗口被拉窄时退化成手机布局，符合预期
2. 不需要 expect/actual，共享层保持纯净
```

### 只列出已经实现的目的地

`DismDestination` 有 8 个值，导航只放已实现的。

```text
放一个点进去显示"敬请期待"的入口  →  用户以为功能坏了
```

同一条原则用于日历导入：`calendarPicker == null` 的平台
**不渲染** Import 按钮。

### 导航项可以带角标

```text
History  ⚠     ← 记录失败时
```

角标传达的是"这个目的地里有需要注意的东西"，详情在页面里。
全屏横幅与角标的取舍见
[Decision History](decision-history.md#全局可见性导航角标1628)。

## 容器与生命周期（16.25）

`DismContainer` 持有长生命周期对象：

```text
Dism              运行时（含数据库连接）
CalendarTextStore 日历缓存
AppDiagnosticSink 诊断出口
fallbackModel     没有任何导入时的兜底
```

### 谁持有它

```text
Android   Application（by lazy，进程内唯一）
桌面       进程（main 里装配一次）
Web        页面（main 里装配一次）
```

Android 上这一点曾经是错的：`Dism` 挂在 `MainActivity` 的字段上，
于是旋转屏幕 / 切深色模式导致 Activity 重建时：

```text
新建一个 Dism  →  新开一个数据库连接
旧的那份丢给 GC  →  连接未关闭
```

### 为什么容器可测

它**不引用** `Context`，也**不引用** `File`。

```kotlin
class DismContainer(
    val dism: Dism,
    val calendarStore: CalendarTextStore,
    val diagnostics: AppDiagnosticSink,
    private val fallbackModel: (Instant) -> DynamicScheduleModel,
)
```

"只创建一次"由平台宿主保证，而"装配出来的东西彼此一致"
由单元测试保证（见 `DismContainerTest`）。

`fallbackModel` 是参数而不是硬编码的示例数据——
将来接上设备日历，兜底策略会变，而容器不该跟着改。

## 相关文档

```text
docs/ui/screens.md           五个页面及其刻意的差异
docs/ui/calendar-import.md   日历导入（16.21 / 16.24）
docs/ui/decision-history.md  决策记录与诊断（16.22 / 16.23 / 16.26）
```
