# Adding a Provider

## 1. 实现契约

```kotlin
class MyScheduleProvider : DismProvider {

    override val id = "my-schedule"
    override val name = "My Schedule"
    override val version = "1.0.0"

    override val capabilities =
        setOf(
            DismProviderCapability.READ,
            DismProviderCapability.SYNC,
        )
}
```

## 2. 产出 observation，而不是领域对象

```kotlin
fun read(context: DismProviderContext):
    DismProviderResult<List<DismProviderObservation>> =
    runCatching {
        fetchFromSource().map { raw ->
            DismProviderObservation(
                providerId = id,
                observedAt = context.requestedAt,
                externalKey = raw.id,
                payload = mapOf(
                    "title" to raw.title,
                    "start" to raw.start,
                ),
            )
        }
    }.fold(
        onSuccess = { DismProviderResult.Success(it) },
        onFailure = { error ->
            DismProviderResult.Failure(
                DismFailure(
                    code = DismFailureCode.OPERATION_REJECTED,
                    message = error.message ?: "provider read failed",
                    operation = "read",
                    details = mapOf("providerId" to id),
                ),
            )
        },
    )
```

要点：

- **不要**直接构造 `Entity` / `Relation`。转换在 Core 侧。
- 平台无关的逻辑不要读系统时间；用 `context.requestedAt`。
- 实现异常要翻译成 `DismFailure`，不要漏给应用。

## 3. 注册

```kotlin
val registry = DefaultDismProviderRegistry()
registry.register(MyScheduleProvider())
```

## 4. 测试清单

```text
✓ registry.all() 保持注册顺序
✓ 重复注册原地替换（版本会变）
✓ 失败路径返回 Failure 而不是抛异常
✓ externalKey 原样保留（它是与外部系统的连接点）
✓ observation 不依赖平台（不要在 commonMain 里用 File / System）
```

## 5. 不要做的事

```text
❌ provider 直接改 Runtime 状态
❌ provider 把自己的 DTO 当公共 API 暴露
❌ 为了"能跑"跳过 Failure 分支
```
