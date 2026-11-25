# Compatibility

## 版本

```kotlin
DismApiVersion.STRING   // "1.1.0"
```

这不是应用版本号，而是 **Public API compatibility version**。

## SemVer 规则

```text
Breaking Public API   →  MAJOR
Additive Public API   →  MINOR
Compatible Fix        →  PATCH
```

## 冻结的公共面

```text
me.moiclathy.dism.api          Dism / DismFactory / DismApiVersion
                               DismCapabilities / DismFailure
                               DismException / DismOperationResult
me.moiclathy.dism.runtime      DismRuntimeApi / RuntimeSnapshot
me.moiclathy.dism.config       DismConfiguration 及子配置
me.moiclathy.dism.diagnostics  DismDiagnosticEvent / Sink / Sinks
me.moiclathy.dism.provider     DismProvider / Registry / Observation
me.moiclathy.dism.platform     DismPlatformEntryPoint 等
me.moiclathy.dism.schedule.api DynamicScheduleFacade 及视图类型
me.moiclathy.dism.schedule.model
                               Model / Item / ItemId / State / Temporal
```

## 明确的过渡 API

```text
DynamicScheduleSession     内部集成会话，不是长期公共 API
DismPersistenceFactory     迁移期基础设施入口
DismRuntimeFactory(persistenceFactory)   迁移期兼容构造
```

它们目前在公共路径上，但**不承诺**长期稳定。

## 怎么保证

`DismPublicBoundaryContractTest` 整份不含
`@OptIn(DismInternalApi::class)`——它就是白名单的可执行形式。
`DismApiCompatibilityContractTest` 锁住 SemVer 形状与
`DismApiVersion.STRING == dism.apiVersion` 的跨层一致。

## Binary API 校验（已接入）

```powershell
.\gradlew.bat :core:checkKotlinAbi --console=plain
.\gradlew.bat :core:updateKotlinAbi --console=plain   # 有意变更时才跑
```

用的是 **Kotlin Gradle Plugin 内置**的 ABI validation，
不是 `kotlinx-binary-compatibility-validator`（BCV 已进入维护状态，
新能力都进了 KGP）。

基线：

```text
core/api/jvm/core.api       JVM
core/api/core.klib.api      iosArm64 / iosSimulatorArm64 / js / wasmJs
```

配置见 `core/build.gradle.kts` 的 `abiValidation { filters { ... } }`：
只 dump 15.x 冻结的九个包，并排除 `@DismInternalApi` 标注的声明。

### klib 的覆盖深度：与 JVM 同为成员级

```text
core/api/jvm/core.api     64024 bytes   成员级（含方法签名）
core/api/core.klib.api    1045 lines    成员级
```

两个引擎都会抓住成员级变更，实测：

```text
给 DismApiVersion 加一个公开方法 abiProbeKlib()
  → <<<ABI has changed>>> 同时出现在 core.klib.api 和 jvm/core.api
```

#### 曾经退化过：`*` 与 `**` 的区别

这里踩过一个坑，值得记住。原来写的是：

```kotlin
byNames.addAll("me.moiclathy.dism.api.*", ...)   // ← 错
```

`byNames` 的通配符语义（[官方文档](https://kotlinlang.org/api/kotlin-gradle-plugin/kotlin-gradle-plugin-api/org.jetbrains.kotlin.gradle.dsl.abi/-abi-filter-set-spec/by-names.html)）：

```text
**   匹配零或多个字符，**含点**
*    匹配零或多个字符，**不含点** —— 用于指定单个类名
```

于是 `…api.*`：

```text
…api.DismCapabilities          ✓ 匹配（* 正好是一个类名）
…api.DismCapabilities.hashCode ✗ 不匹配（* 跨不过那个点）
```

后果是两个引擎表现**不一致**：

```text
JVM 引擎     匹配到的类型，其成员一并保留 → 看起来正常
klib 引擎    成员各自参与匹配，没匹配到就丢弃 → 退化成 173 行类型清单
```

修法就是一个字符：`.*` → `.**`。

```text
               修复前        修复后
core.klib.api  173 行        1045 行
jvm/core.api   47679 bytes   64024 bytes   ← JVM 其实也在少覆盖成员
```

**教训**：不要从"某个引擎看起来正常"推断过滤器没问题。
两个引擎对同一份过滤器配置的解释可能不同。

## 目标覆盖：Android 不在 ABI 校验范围内

### 事实

```text
实际生成的 dump
  core/build/kotlin/abi/jvm/core.api       JVM 目标
  core/build/kotlin/abi/core.klib.api      iosArm64 / iosSimulatorArm64 / js / wasmJs

没有 android 的 dump。
```

后果：

```text
androidMain 里的公共 API 增删改  →  checkKotlinAbi **不会**察觉
```

例如 `AndroidDismFactory` 是 16.14 新增的公共 API，
而当时 `checkKotlinAbi` 通过——不是因为它没问题，
是因为**它没在看 Android**。

### 为什么补不上（已查证，不是放弃）

三条独立证据：

**1. DSL 层面没有 Android 的位置**

`AbiValidationMultiplatformVariantSpec` 的全部成员：

```java
AbiValidationKlibKindExtension getKlib();
void klib(Action<AbiValidationKlibKindExtension>);
```

只有一个 `klib` kind。没有 jvm kind，也没有 android kind。

**2. 曾经可能的位置已被移除**

`AbiValidationExtension.variants`（按目标命名的容器）确实存在过，
但当前版本报错：

```text
'val variants: NamedDomainObjectContainer<AbiValidationVariantSpec>'
is deprecated. Variants DSL was removed and is no longer supported.
```

所以"按目标启用 Android"这条路已经关闭。

**3. 官方文档的模型不含 Android**

> Because Kotlin/Android projects and multiplatform projects with an
> Android target don't publish JAR files, this feature doesn't apply to them.

（指 `binariesSource = MAVEN_PUBLICATIONS`，但反映了同一件事：
Android 目标不在 KGP 的 ABI 发布模型里。）

### 可选的处理方式

```text
a. 接受现状 + 让 androidMain 的公共面保持极小
   目前 androidMain 只有 1 个公共类型（AndroidDismFactory）
   它的实现是 1 行委托到与 JvmDismFactory 共享的 DismBootstrapper 路径，
   而那条路径有 JvmDismFactoryTest 覆盖
   → 行为风险低，风险主要是"有人无意中加了公共 API"

b. 为 Android 单独引入 legacy BCV
   kotlinx-binary-compatibility-validator 能处理 Android 目标，
   但它已进入维护状态，且会与 KGP 的 dump 共存（两套 *LegacyAbi 任务）
   → 收益是字节码级覆盖，代价是两套 ABI 工具

c. 把 Android 的公共契约挪出 androidMain
   由于 Context 是平台类型，公共入口无法放进 commonMain
   → 除非改成不暴露平台的装配方式，否则不可行
```

**当前选择 (a)**，并把这条限制写在案。
如果哪天 androidMain 的公共 API 超过一个类型，应该重新评估 (b)。

### 与 `keepLocallyUnsupportedTargets` 的区别

不要混淆：那个开关管的是**本机编译不了的目标**（如 Windows 上的 iOS），
KGP 会用可用目标**推断**它们的 ABI 变化。

```text
iOS（本机不支持）  →  被推断，仍参与校验
Android（本机支持）→  根本不参与
```

Android 的问题不是"编译不了"，而是"不在校验模型里"。

## 依赖可复现性（已接入）

两层，文件在仓库里就自动生效：

```text
gradle/verification-metadata.xml     sha256 校验
core/gradle.lockfile                 版本锁定
server/gradle.lockfile
app/shared/gradle.lockfile
settings-gradle.lockfile
```

更新方式：

```powershell
# 依赖变化后
.\gradlew.bat <tasks> --write-verification-metadata sha256

# 版本变化后
.\gradlew.bat <tasks> --write-locks
```

### 两个已知边界

**AAPT2 是 trusted-artifact 豁免**
AGP 把它解析进 detached configuration；它不是运行期依赖，也不随库分发。
豁免是窄口径的（一个 group + name）。
不用手写 checksum 是因为手写条目会在重新生成时被静默丢掉——
文件看起来还在，其实已经不覆盖 AAPT2 了。

**`--refresh-dependencies` 会失败**
依赖经 Aliyun 镜像解析，强制刷新拿到的元数据可能与基线不一致。
常规构建路径不受影响。需要刷依赖时应当同时重新记录 checksum，
而不是绕过校验。

## 还没做的

```text
clean checkout build
release artifact
```

**不要**在缺少构建环境时自己造一个反射扫描器冒充它们。
