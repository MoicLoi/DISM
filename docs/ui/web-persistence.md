# Web 持久化（16.34）

## 问题

浏览器没有文件系统，SQLDelight 的 driver 在 Web 上不可用。
所以在 16.34 之前：

```text
刷新页面  →  日历还在（localStorage）
            决策历史清空（内存）
```

## 为什么不是 IndexedDB

我上一轮把这件事说成"可以做的下一项"。动手前先查了依赖，发现：

```text
kotlinx-browser 提供   window / document / localStorage / sessionStorage
kotlinx-browser 不含   IndexedDB
```

于是 IndexedDB 意味着**手写整套 externals**：

```text
IDBFactory / IDBDatabase / IDBObjectStore / IDBTransaction
IDBRequest / IDBOpenDBRequest / IDBVersionChangeEvent / 各种回调
```

几百行声明，换来的是更大的配额（localStorage 约 5MB，
IndexedDB 通常几十 MB 起）。

对**决策历史**这个量级——按"决策变化"追加（16.22），
单条几百字节——5MB 足够。

```text
结论：用 localStorage，而不是手写一套无法验证的 FFI
```

要真超了配额，应该分片键（`snapshot:<id>`）或换后端，
而下面这个接口允许那样演进。

## 关键设计：把逻辑从绑定里剥出来

```text
StringStorage（接口）              core/commonMain
JsonSnapshotStore（JSON + 查询）    core/commonMain   ← 可在 JVM 上测
LocalStorageStringStorage（绑定）   core/webMain      ← 很薄
WebDismFactory（装配）              core/webMain
```

如果按"只有 Web 用它"的直觉把存储放进 `webMain`，
它的每一条逻辑就只能靠浏览器验证——而当前**没有任何浏览器自动化**，
等于全部不可验证。

抽成 `StringStorage` 之后：

```text
编解码、最新一条的取舍、query 的排序与截断、损坏数据的处理
    → core/commonMain，14 条 JVM 测试
把键值转发到 localStorage
    → 不需要被测试
```

> **不可验证的部分被压到最小。**

这与把 `CalendarTextStore` 抽出来让三端共用是同一个手法。

## 复用 SQLDelight 的扁平列

存储格式不是新发明的，而是复用 SQLDelight 那套列
（`SqlDelightSnapshotValues`，加一个 `@Serializable` 即可）：

```text
编解码已在 ScheduleDecisionSqlDelightCodec 里有一份
  → 复用它可以避免"两份字段映射慢慢漂移"
```

代价是把 SQLite 的列名当成了 JSON 的键——名字不好看，
但换来"映射只有一处"，而且三个实现（内存 / SQLite / JSON）
的语义能被同一批断言覆盖。

## 测试揭示的一个不变量

第一版夹具只改了外层的 `itemId`，内层 `item.itemId` 留着原值。
往返测试立刻失败，失败信息指向内层字段：

```text
存入:  itemId=item-a, item.itemId=item-1
读出:  itemId=item-a, item.itemId=item-a
```

原因：扁平列表示里**只有一列** `item_id`，内层那个由它派生。
所以：

```text
itemId == item.itemId   是这个表示方式隐含的不变量
```

任何基于那套列的实现（包括 SQLDelight 那个）都有同样的性质。
**不是存储的 bug，是夹具违反了不变量。**

## 只换了一个端口，这是刻意的

`DismPersistence` 有 12 个端口。只换 `scheduleSnapshots`：

```text
entities / evidence / relationStates / transitions
    ↑ 由语义层写入，而应用目前只做"导入日历 + 评估决策"
    → 它们在浏览器里始终是空的
```

为空的表实现持久化换不来任何用户可见的效果，
却要写 11 份无法验证的代码。

而 `scheduleSnapshots` 是**决策历史**——History 页读的就是它。

### 代价要说清楚

```text
刷新页面  日历还在        ✅
          决策历史还在    ✅ 16.34 新增
          语义状态没了    ⚠ 内存，但它本来也没被写过东西
```

等应用真的开始产生语义状态，这里就要补上对应端口，
并重新评估 localStorage 是否还合适。

## 踩到的 KMP 陷阱（这个项目第三次）

测试第一版用了 `runBlocking`：

```text
runBlocking 只在 JVM / Native 上有，JS 没有
而测试在 commonTest，会为 JS 编译
```

`:core:jvmTest` 通过，`:core:compileTestKotlinJs` 失败。
改用多平台的 `runTest`。

```text
前两次   putIfAbsent（16.32）  toSortedSet（更早）
共同点   都是"在 JVM 上顺理成章"的 API
教训     写 common 代码前先确认它在 common 里存在；
         而**跨目标编译**是发现它的唯一手段
```

## 锁状态的更新

给 `:core` 加 `kotlinx-browser` 后，锁文件需要更新。

用的是上次踩坑后定下的流程：

```text
1. help --write-locks                    覆盖 buildscript 配置
2. 完整任务集 --write-locks              覆盖所有编译/测试配置
3. 纯验证跑一遍                          确认自洽
```

**不要用子集任务集写入**——未解析到的配置条目会被从文件里删掉
（16.23 那次就是这么坏的）。

## 验证程度（16.35 已在真实浏览器里跑过）

原先写在这里的是"⚠ 没有验证过"。**那已经过时**——
Kotlin 自带的浏览器测试管线（Karma + ChromeHeadless）是可用的：

```text
:core:jsBrowserTest      251 套件 / 1290 测试 / 0 失败
:core:wasmJsBrowserTest  （同上的 wasmJs 版本）
```

也就是说 commonTest 的**全部**测试在真实浏览器里跑了一遍，
包含 `JsonSnapshotStoreTest` 的 14 条存储语义测试。

### 而"刷新后历史还在"这条承诺

它需要的不只是存储语义，还有 `localStorage` 绑定与 Web 装配。
所以补了一组**只在浏览器目标上编译**的测试
（`core/src/webTest`）：

```text
localStorageStorageRoundTripsInTheBrowser
historySurvivesANewDismInstance        ← 这就是"刷新"的等价物
theBatchLookupSeesPersistedHistory
anEmptyBrowserStorageStartsClean
```

`historySurvivesANewDismInstance` 直接模拟刷新：

```text
用 WebDismFactory 建一个 Dism，写一条决策
丢掉它
再建一个        ← 等价于刷新页面
看历史还在不在
```

结果：

```text
jsBrowserTest     WebLocalStoragePersistenceTest 4/4 通过
wasmJsBrowserTest WebLocalStoragePersistenceTest 4/4 通过
                  （ChromeHeadless154.0.0.0, Windows10）
```

`anEmptyBrowserStorageStartsClean` 是防"假通过"的：
没有它，上面那条可能因为上一个用例留下的数据而通过。

### 仍然没有验证的

```text
真实的 Compose 界面在浏览器里长什么样      ← 我看不到截图
Import 按钮能否弹出文件选择框
手工操作下的端到端流程
```

即"能跑、能存、能读"已验证，**"看起来对不对"没有**。

## 首次运行 JS 工具链的代价（值得记下来）

跑 `jsBrowserDevelopmentRun` / `*BrowserTest` 会触发**首次**
下载 Node 发行版，而它们不在校验元数据里，门禁会拦住：

```text
Could not determine the dependencies of task ':kotlinNpmInstall'.
  Could not resolve all dependencies for configuration ':core:jsNpmAggregated'.
    Resolved 'org.jetbrains.kotlinx:kotlinx-browser-js:0.5.0'
    which is not part of the dependency lock state
```

修法是**同一次调用里同时写锁与校验元数据**（单独写会互相阻塞）：

```powershell
.\gradlew.bat :kotlinNpmInstall `
    --write-locks --write-verification-metadata sha256
```

而 js 与 wasmJs 用的是**不同的 Node 版本**：

```text
js        org.nodejs:node:24.10.0
wasmJs    org.nodejs:node:25.0.0
```

两个都要单独记录一次。此外 JS 资源解析还需要
`components-resources` 的 `.kotlin_resources.zip`。

```text
教训：我此前的"全量任务集"从来没有包含 npm / webpack 那条流水线，
      所以锁与校验元数据对它是**一直有缺口**的——
      跑一次浏览器测试就会暴露。
      标准验证命令里现在应该带上 *BrowserTest。
```

