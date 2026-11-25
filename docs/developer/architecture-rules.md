# Architecture Rules

这一份是 15.x 冻结下来的**硬规则**。违反它们的 PR 应该被拒绝。

## 规则 1：Persistence 不是 App API

```text
DismRuntime
  ├── persistence   private
  └── restored      private
```

应用不允许出现：

```text
❌ runtime.persistence.entities
❌ runtime.persistence.relationStates
```

理由：一旦应用能直接拿 Store，把 SQLDelight 换成别的 driver
或改成远程同步就会把 App 绑死。

## 规则 2：产品入口零 Persistence 参数

```kotlin
DismFactory.create()                        ✅
DismFactory.create(DismConfiguration)       ✅
DismFactory.create(DismPersistenceFactory)  ❌
DismFactory.create(DismPersistence)         ❌
```

一个 Public API 的**签名**不能出现 internal 类型。
实现内部可以引用它。

## 规则 3：不要伪造能力

```kotlin
DismStorageMode.LOCAL   // 需要平台 Provider，否则报错
```

**绝不**静默退化成内存存储。一个"看起来成功、其实没落盘"的
LOCAL 比一个启动就报错的 LOCAL 危险得多。

同理：`capabilities` 必须反映真实 configuration，
`DiagnosticsConfiguration.enabled = false` 必须真的静默 sink。

## 规则 4：标记边界，但先不要大规模 `internal`

```kotlin
@DismPublicApi    承诺兼容
@DismInternalApi  不承诺兼容（WARNING 级）
```

`:core` 自己 opt-in 了 `DismInternalApi`（见 `core/build.gradle.kts`），
所以模块外的消费者才会看到警告。

不要机械地把 990 个文件全改成 `internal`——KMP 多 source-set
下会引发大批 visibility failure。

## 规则 5：时钟由调用方传入

决策引擎是纯函数：`f(input, previous, now)`。

```text
now / replayedAt / verifiedAt / checkedAt / generatedAt
```

都从外部传入，引擎内部不读系统时间。否则同一输入会产生不同结果，
重放与验证就没有意义。

诊断事件不在这条线上（它不参与决策），但仍应显式传 `timestamp`。

## 规则 6：确定性排序

任何"要写进指纹 / Provenance / 差异列表"的东西都必须排序。

```kotlin
// ❌ JVM 能过、JS/Wasm 编译不过，且顺序依赖哈希布局
set.toSortedSet()

// ✅
list.sortedBy { it.value }
```

同样注意：`mapOf(...)` 的遍历顺序是**实现细节**，
用于稳定输出时要显式 `sortedBy { it.key }`。

## 规则 7：不要伪造 Provenance 节点

只有调用方真的提供了事实，才生成对应节点：

```text
没有 Trace        → 没有 TRACE 节点
没有 override     → 没有 HUMAN_OVERRIDE 节点
```

并如实降低 `completeness`。
