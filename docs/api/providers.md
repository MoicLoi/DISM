# Providers

Provider 是外部源或服务的适配器。

```text
Internal Domain
      ↑
External Adapter   ← 你在这里
      ↑
Provider（喜鹊儿 / 课程表 / ICS / 第三方日历）
```

## 契约

```kotlin
interface DismProvider {
    val id: String
    val name: String
    val version: String
    val capabilities: Set<DismProviderCapability>
}
```

`version` 不是装饰：线上出现"某个 provider 行为变了"时，
必须能回答是适配器改了还是源站改了。

## 不拥有领域状态

Provider **不修改** DISM Runtime，也不直接把 DTO 塞进 Core。
它产出 `DismProviderObservation`：

```kotlin
data class DismProviderObservation(
    val providerId: String,
    val observedAt: Instant,
    val externalKey: String? = null,
    val payload: Map<String, String> = emptyMap(),
)
```

它是 source-oriented 的，本身不是 Entity 或 Relation。
转换由 Core 侧完成。

## 结果

```kotlin
sealed interface DismProviderResult<out T> {
    data class Success<T>(val value: T) : DismProviderResult<T>
    data class Failure(val error: DismFailure) : DismProviderResult<Nothing>
}
```

Provider 抛出的实现异常不会直接暴露给应用，
而是翻译成 `DismFailure`。

## Registry

```kotlin
val registry = DefaultDismProviderRegistry()
registry.register(provider)
registry.find("quequeer")
registry.all()          // 注册顺序，不是哈希顺序
```

注册/注销会发出 `DISM.PROVIDER.REGISTERED` / `UNREGISTERED`。

写一个 provider 见
[developer/adding-a-provider.md](../developer/adding-a-provider.md)。
