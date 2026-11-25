# Diagnostics

DISM 不绑定任何日志框架，只发射结构化事件。

```kotlin
fun interface DismDiagnosticSink {
    fun emit(event: DismDiagnosticEvent)
}
```

## 接入

```kotlin
val sink = DismDiagnosticSinks.inMemory()

val dism = DismFactory.create(
    DismConfiguration(
        diagnostics = DismDiagnosticsConfiguration(sink = sink),
    ),
)

sink.snapshot()   // 已经能读到 DISM.BOOTSTRAP.STARTED / READY
```

现成的 sink：

```text
DismDiagnosticSinks.none               丢弃
DismDiagnosticSinks.inMemory(capacity) 有界内存收集（测试/开发）
```

## Code 规范

```text
DISM.<COMPONENT>.<EVENT>
```

已定义的：

```text
DISM.BOOTSTRAP.STARTED / READY
DISM.STORAGE.RESOLVED / UNAVAILABLE
DISM.PROVIDER.REGISTERED / UNREGISTERED
DISM.PROVIDER.REQUEST.STARTED / COMPLETED / FAILED
DISM.PROVIDER.SYNC.STARTED / COMPLETED / FAILED
```

用常量而不是散落的字符串字面量——日志聚合按 code 分组，
一次拼写错误就会在监控里凭空多出一个"新事件"。

## DismDiagnostics 是 internal

应用配置 sink，内部组件通过 `DismDiagnostics` 发射事件。
应用不应该自己构造它。
