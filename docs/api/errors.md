# Errors

## 两种失败

```text
DismOperationResult<T>   失败是正常业务分支时使用
DismException             调用方无法继续时抛出
```

## DismFailure

```kotlin
data class DismFailure(
    val code: DismFailureCode,
    val message: String,
    val operation: String? = null,
    val details: Map<String, String> = emptyMap(),
)
```

**按 `code` 分支，永远不要解析 message。**

```kotlin
when (failure.code) {
    DismFailureCode.STORAGE_UNAVAILABLE -> ...
    DismFailureCode.REPLAY_UNAVAILABLE -> ...
    DismFailureCode.UNSUPPORTED_CAPABILITY -> ...
    ...
}
```

## 稳定的诊断字符串

```kotlin
DismFailureFormatter.format(failure)

// STORAGE_FAILURE [writeSnapshot]: Storage operation failed. (entity=schedule)
```

`details` 按 key 排序输出，保证同一份失败永远得到同一个字符串。

## 不要把所有 require 都改成 DismException

构造参数校验（例如 `DynamicScheduleHistoryQuery(limit = -10)`）
属于**编程错误**，保留 `IllegalArgumentException` 更合适。

`DismException` 留给"运行时环境导致的、应用必须处理的失败"。
