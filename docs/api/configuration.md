# Configuration

```kotlin
data class DismConfiguration(
    val storage: DismStorageConfiguration = DismStorageConfiguration(),
    val runtime: DismRuntimeConfiguration = DismRuntimeConfiguration(),
    val diagnostics: DismDiagnosticsConfiguration = DismDiagnosticsConfiguration(),
)
```

## Storage

```kotlin
enum class DismStorageMode { IN_MEMORY, LOCAL }

data class DismStorageConfiguration(
    val mode: DismStorageMode = DismStorageMode.IN_MEMORY,
    val databaseName: String? = null,   // 逻辑名，不是文件路径
)
```

- `databaseName` **不是**文件系统路径。路径解析属于平台 bootstrap。
- `LOCAL` 需要平台绑定，否则启动即报错。见 [errors.md](errors.md)。

## Runtime

```kotlin
data class DismRuntimeConfiguration(
    val enableAttention: Boolean = true,
    val enableDynamicSchedule: Boolean = true,
    val enableAdaptation: Boolean = true,
)
```

这些开关**真的影响** `dism.capabilities`：

```kotlin
val dism = DismFactory.create(
    DismConfiguration(
        runtime = DismRuntimeConfiguration(enableAttention = false),
    ),
)

dism.capabilities.attention   // false
```

## Diagnostics

```kotlin
DismConfiguration(
    diagnostics = DismDiagnosticsConfiguration(
        enabled = true,
        sink = DismDiagnosticSinks.inMemory(),
    ),
)
```

`enabled = false` 会真的静默 sink，而不是仅仅设置一个标志位。
