# DISM 交接文档

> 生成于 UI 阶段（16.x）收口点。目的是让**新的上下文**能直接接手，
> 不必重新发现那些已经付过代价的结论。

## 一、一句话状态

`:core` 编译通过、门禁全绿、UI 五个页面可跑、桌面端真实落盘、
ICS 导入链路已打通、**发布包已生成并真的跑过**；剩余缺口见第五节。

## 二、当前数字（以此为准）

```text
:core       271 suites / 1352 tests / 0 failures
:core       jsBrowserTest / wasmJsBrowserTest 全部通过
:app:shared  12 suites /   74 tests / 0 failures

公共 API 版本   1.5.0
core/api/jvm/core.api       64136 bytes（成员级）
core/api/core.klib.api      1051 lines（成员级）

门禁
  ABI            :core:checkKotlinAbi            通过（已接入 :core:check）
  依赖校验       gradle/verification-metadata.xml 通过（1112 组件 / 2075 artifacts）
  版本锁定       全部 6 个模块 + settings 各有 gradle.lockfile
                 core / server / app:androidApp / app:desktopApp / app:shared / app:webApp

发布包（16.36）
  build/github-release/dism-1.5.0/
    source      dism-1.5.0-source.zip          1501 文件 / 12.9 MB
    artifacts   desktop-windows-x64.zip        78 MB（含裁剪 runtime）
                web-js.zip / web-wasm.zip     18.6 / 15.3 MB
                debug.apk                      14 MB
    说明        README.md / COMMANDS.md / RELEASE-NOTES.md / SHA256SUMS.txt
  已验证        桌面包解压后真的启动（窗口 DISM、BOOTSTRAP.READY、写 dism.db）
                Web 包在 Chromium 158 里挂载成功、控制台无页面错误
                连打两次除 VERSION.txt 外逐字节相同
```

验证命令（每次改动后跑）：

```powershell
.\gradlew.bat :core:jvmTest :app:shared:jvmTest :core:checkKotlinAbi `
              :core:compileKotlinJs :core:compileKotlinWasmJs `
              :core:compileAndroidMain :core:packageAndroidMainResources `
              :core:compileTestKotlinJs `
              :core:jsBrowserTest `
              :core:wasmJsBrowserTest `
              :app:shared:compileKotlinJs :app:shared:compileKotlinWasmJs `
              :app:shared:compileAndroidMain `
              :app:desktopApp:compileKotlin :app:androidApp:compileDebugKotlin `
              :server:compileKotlin --console=plain
```

## 三、本会话完成了什么

### 15.x 收尾

```text
15.1 – 15.15   全部合并完成
发布门禁 1     Binary API validation（KGP 内置 abiValidation）
发布门禁 2     依赖可复现性（verification-metadata + dependencyLocking）
```

两条门禁都做过**破坏性验证**，不是"配上了就算"：

```text
往 DismFactory 加公开方法   → <<<ABI has changed>>> + 精确 diff
篡改一个 sha256             → 24 artifacts failed verification
删掉一条 jvmCompileClasspath 锁 → not part of the dependency lock state
```

### 16.x UI 阶段

```text
16.1  展示层：DismPresenter + 三个纯投影（Home / Schedule / Attention）
16.2  设计系统：DismTheme + DismSpacing
16.3  导航壳：窄屏底部栏 / 宽屏左侧导轨（BoxWithConstraints 按宽度切换）
16.4  Today 页
16.5  Schedule 页（保留终态条目）
16.6  Attention 页（如实标注"无解释"）
16.7  Semantic State 页（桌面/Web）
16.8  History 页（按 item 查询）
16.9  Settings 页（只读能力展示）
16.14 平台落盘：JvmDismFactory / AndroidDismFactory
16.21 ICS 导入：DismScheduleImport.fromIcs
16.22 决策记录策略：DecisionRecorder（变了才写历史）
16.23 记录失败接入诊断：AppDiagnosticSink + DECISION_RECORD_FAILED
16.24 三端日历导入接线：桌面(AWT) / Android(SAF) / Web(input file)
16.25 Application 级容器：DismContainer（三端共用装配）
16.26 记录失败对用户可见：History 页横幅 + recordingError
16.27 文档：docs/ui/ 四篇 + README 文档索引
16.28 记录失败的全局可见性：导航角标 + 无障碍描述
16.30 无障碍：对比度门禁 + 配色单一来源 + 标题语义
16.31 性能：snapshot 从热路径移出（按需加载库级状态）
16.32 性能：消除 N+1 查询（批量 latestDecisions，85ms → 2ms）
16.33 响应式：断点模型 + 导航规则纯函数 + 内容宽度上限
16.34 Web 持久化：JSON + localStorage 存决策历史（WebDismFactory）
16.35 浏览器验证：jsBrowserTest / wasmJsBrowserTest 真跑通（ChromeHeadless）
16.36 GitHub 发布包：tools/package-github-release.ps1
      source + desktop zip + web js/wasm zip + debug apk + COMMANDS.md + SHA256SUMS
      同时修掉两个只有"打包并真的运行"才会暴露的问题（见第七节 9、10）
```

### 顺带修掉/发现的问题

```text
1. evaluate() 会持久化，UI 用它导致每次打开都写一条历史
   → 新增只读入口 preview()，公共 API 1.0.0 → 1.1.0

2. 落盘在 16.14 之前根本做不到
   → DismPersistenceProvider.create() 返回 internal 的 DismPersistence，
     app 模块无法实现它。新增平台公共工厂，1.1.0 → 1.2.0

3. AndroidDatabaseDriverFactory 硬编码库名 "dism.db"
   → 配置 databaseName 被静默忽略。改为构造参数

4. History 排序按格式化字符串比较
   → "Wed, Sep 9" 会先比星期缩写。改为在领域对象上按 capturedAt 排

5. DemoSchedule 手工构造模型，绕过 11.x 真实链路
   → 删除，改为 SampleCalendar.ics() + 真实导入

6. 加载路径**确认**每次启动只跑一次（16.29 实测）
   用探针量过：
     App composition #1 → LaunchedEffect launched → load entered
       → recordHistoryIfChanged attempting → caught → 1 条 WARN
     App composition #2（重组，effect 未重启）
   即：一次启动 = 一次 composition 作用域（含一次重组）
        = 一次 effect = 一次 load = 一次记录尝试
   ⚠ 曾经在真实运行里看到过**两条相同 WARN**，但加探针后
     无法复现，上述测量也否证了"load 跑两次"的推断。
     我不知道那次是什么造成的。若再次出现，用同样的探针定位：
     在 App、LaunchedEffect、DismPresenter.load、
     recordHistoryIfChanged 四处各打一行 [PROBE]。
     注意：写入成功时第二次尝试是空操作，
     所以"历史没重复增长"并不能证明只跑了一次。

7. klib ABI 只到类型级（**已修复**）
   → 过滤器写成了 `.*` 而不是 `.**`
     byNames 的 `*` 不跨点，匹配不到成员；`**` 才跨
   → klib 173 → 1045 行；JVM 47679 → 64024 bytes
   → 破坏性验证：加一个公开方法，两个引擎都报警
```

## 四、关键设计决定（重新推导代价很高）

### 一次评估，三个页面共享

```text
DismPresenter.load()   评估一次
      ↓
HomeProjection / ScheduleProjection / AttentionProjection   纯函数
```

不这么做的话：同一模型算三遍，且三次之间若有状态变化，
界面会出现互相矛盾的视图。

### 展示层不重新判断"是否需要提醒"

Attention 页只过滤 `attentionRequired == true`（决策的输出）。
如果在 UI 里再写一遍"优先级 > 0.7 就该提醒"，
就会出现两套注意力规则，不一致时没人知道该信谁。

### Today 做减法，Schedule 做加法

```text
Today     过滤终态（只看还要做的）
Schedule  保留终态（用户要确认"那件事我做完了"）
```

这不是不一致，是不同问题。测试 `scheduleKeepsTerminalItemsButTodayDoesNot` 锁住。

### 错误分类规则

```text
调用方错误（配置要求了做不到的事）  → 抛 DismException
用户数据错误（文件不是日历）        → 返回带 errors 的结果
```

理由：抛异常会丢掉 warnings（3 好 1 坏的文件应该导入 3 条并告知第 4 条），
且"用户选错文件"是常规情况，不该迫使调用方写 try/catch。

### UI 只列已实现的目的地

`DismDestination` 有 8 个，导航只放已实现的。
点进去显示"敬请期待"的入口比没有入口更糟。

### 环境/时钟纪律（13.15 沿用）

```text
now 由调用方传入，Presenter 内部不读时钟
timeZone 显式可传，否则 UiState 随运行机器变化、单测无法断言
```

## 五、已知缺口（诚实清单）

```text
1. （已关闭）klib ABI 只到类型级
   根因：过滤器写成 `.*`，而 byNames 的 `*` 不跨点 → 匹配不到成员
   修法：改成 `.**`。klib 173 → 1045 行，两个引擎都到成员级
   已用"加一个公开方法"做破坏性验证，core.klib.api 与 jvm/core.api 都报警
   详见 docs/developer/compatibility.md

2. Android 公共 API 不在 ABI 校验范围（**已查证：KGP 内置工具补不上**）
   core/api/jvm/core.api 只含 JVM 主源集
   core/api/core.klib.api 只含 iosArm64 / iosSimulatorArm64 / js / wasmJs
   → AndroidDismFactory 是新增公共 API，checkKotlinAbi 通过是因为它没在看 Android
   证据：
     AbiValidationMultiplatformVariantSpec 只有 klib kind，没有 android kind
     variants 容器 DSL 已被移除（"Variants DSL was removed..."）
     官方文档：multiplatform projects with an Android target 不在此发布模型里
   注意：这与 keepLocallyUnsupportedTargets 不是一回事——
     那个管"本机编译不了的目标"（iOS），会被推断；Android 是根本不参与
   当前选择：接受 + 保持 androidMain 公共面极小（现仅 1 个类型）
   若将来 androidMain 公共 API 超过一个类型，考虑引入 legacy BCV
   详见 docs/developer/compatibility.md「目标覆盖」

3. Android 落盘仅编译验证
   无设备/模拟器。"应用私有目录真的可写""卸载重装后行为"未验证

4. iOS 完全未做
   IosDatabaseDriverFactory 存在但没有公共工厂；Windows 上 iOS 目标被禁用
   不要用 kotlin.native.ignoreDisabledTargets=true 去消掉那条警告

5. （已关闭）真实日历来源：三端都已接（16.21 + 16.24）
   ```text
   桌面      AwtCalendarPicker          ~/.dism/calendar.ics
   Android   AndroidCalendarPicker(SAF) filesDir/dism-calendar.ics
   Web       WebCalendarPicker(input)   localStorage
   ```
   共享：CalendarImporter（选择→解析→缓存→反馈）+ StartupCalendar
   （缓存→重新解析→回退），存储通过 CalendarTextStore 接口抽象。
   ⚠ 验证程度不同：
     桌面      真实运行验证过（含端到端）
     Android   仅编译验证（无设备/模拟器）
     Web       已在真实浏览器里验证落盘（16.35 + 16.34）
   ⚠ Web 的决策历史：16.34 之前只在内存里，刷新即清空。
     现在走 LocalStorageStringStorage → JsonDynamicScheduleDecisionSnapshotStore，
     与桌面/Android 同一套快照格式。见 docs/ui/web-persistence.md。
     SQLDelight 在 Web 上仍然不可用（浏览器无文件系统），这是有意的分工：
     关系型落盘给桌面/Android，快照 JSON 给 Web。

5c. 响应式布局（16.33）
   断点       COMPACT <600 / MEDIUM 600–839 / EXPANDED ≥840
              （对齐 Material 3 window size class）
   导航规则   navDestinationsFor(size) —— 可测的纯函数
   内容宽度   宽屏封顶 840dp，避免过长的行导致读串行
   顺带修掉   MEMORY 是 primary=true 却一直出现在导航里、
              点进去显示"未实现"；注释还写着"Memory 不在导航里"。
              现在 implemented 是独立字段，导航只列已实现的。
   ⚠ 只有逻辑层验证；没有在实际窄/宽/超宽窗口里看过
   ⚠ 两栏布局未做（需要条目详情页才成立）

5d. （16.36 新增）发布包的"画得对不对"没法验证
   能证明的：Web 产物在 Chromium 里挂载成功、控制台 0 条页面错误、
             所有静态资源 200；桌面 exe 解压后启动并落盘。
   不能证明的：画面是否正常。截图能拿到（build\web-artifact.png），
             但当前模型没有图像输入能力，看不了。
             要补只能靠人眼看，或者换一个能读图的模型。
   ⚠ 不要把"控制台没报错"当成"UI 显示正常"

5b. （已关闭）History 页在真实应用里永远是空的
   原因：1.1.0 把展示层从 evaluate 换成 preview 之后，应用从不写历史。
        修掉"每次加载都写"的同时，引入了"从不写"。
   修法（16.22）：预览后与 latestDecision 逐条比对，**变了才 evaluate**。
        策略放在 app/shared 的 DecisionRecorder，不进 Core（那是产品决策）。
   ```text
   每次加载都写  →  历史无限增长
   从不写        →  历史永远为空
   变了才写      →  历史 = 决策何时改变的日志      ← 现在
   ```
   真实应用端到端验证（查 db 里 e2e-1 的出现次数）：
   ```text
   首次运行（无记录）  0 →  9   记了 1 条
   重启，模型未变      9 →  9   没记
   改内容后重启        9 → 18   记了 1 条
   ```
   16.23 补上诊断出口：失败会发一条 WARN 事件
   （code = DECISION_RECORD_FAILED，带 errorType 属性）。
   真实应用验证（把 dism.db 设为只读）：
   ```text
   [DISM WARN] presentation/recordDecision DECISION_RECORD_FAILED:
     Failed to record the decision: [SQLITE_READONLY]
     Attempt to write a readonly database
   ```
   窗口仍正常响应 —— "吞掉但报告"的降级生效。
16.26 已补上用户可见的部分：History 页会显示横幅
   "New decisions could not be recorded / This list may be
   missing recent entries" 并附上原始原因（例如 SQLITE_READONLY）。
   状态字段：HistoryUiState.recordingError / historyMayBeIncomplete。
   它反映的是"**现在**记录是否可用"而非累计错误：
   判定是否记录是拿当前决策与已存下来的比对，
   所以一次失败的写入会让下次加载重试，成功即自动清除提示。
   16.28 补上全局可见性：History 导航项带角标（任何页面都能看到），
   并带无障碍描述（读屏念的是"History may be incomplete…"
   而不是"History 感叹号"）。

6. （已关闭）Dism 归属是 Activity 字段 → 已改为 Application 级容器
   ```text
   Android   DismApplication.container（by lazy，进程内唯一）
   桌面       进程（main 里装配一次）
   Web        页面（main 里装配一次）
   ```
   修掉的问题：Activity 因旋转 / 深色模式切换重建时，
   之前会新建 Dism（新数据库连接），旧的那份被丢给 GC 且连接未关。
   现在长生命周期对象（Dism / 日历缓存 / 诊断 sink）都在容器里，
   Activity 只持有与界面同生命周期的 SAF launcher 与 picker。
   DismContainer 不引用 Context / File，所以在 commonTest 里可测（5 个测试）。
   ⚠ Android 侧仍只有编译验证。

7. （已关闭）16.x 文档
   新增 docs/ui/ 四篇：architecture / screens / calendar-import /
   decision-history，覆盖 16.1–16.9 与 16.21–16.26。
   16.14 在 docs/release/16.14-platform-integration.md。
   README 补上了「Documentation」索引（之前没有索引，
   文档等于找不到），并校验了所有链接可解析。
   docs/ 现有 26 个文件。

7b. （已关闭）N+1 查询
   `DecisionRecorder` 曾对每个条目一次 `latestDecision(itemId)`。
   实测（真实 SQLDelight，200 条）：n-lookups=85ms，preview=1ms
   —— 比它守护的评估链贵 85 倍，且每次启动都跑。
   修法（16.32）：公共 API 加批量入口 `latestDecisions(itemIds)`
   （一条相关子查询，非循环），1.3.0 → 1.4.0。
   修后：batch=2ms。
   注意：公共 API 的"没记录过的条目不出现"是契约的一部分。

7c. 相邻教训：KMP common 里不能用 JVM-only API
   `putIfAbsent` 在 JVM 上编译通过（LinkedHashMap 在 JVM 上
   就是 java.util.LinkedHashMap），但 common 里不存在。
   只有跑跨目标编译才暴露（本案由 :core:checkKotlinAbi 触发的
   iOS 编译发现）。同类亏此前吃过一次（toSortedSet）。

8. --refresh-dependencies 会失败
   依赖经 Aliyun 镜像解析，强制刷新拿到的元数据与基线不一致
   常规构建不受影响；刷依赖时须同时重新记录 checksum
```

## 六、下一步建议

按性价比排序：

```text
A. 真实日历来源（文件选择 / 权限）
   产品决策多，建议先定"导入通过什么交互触发"。
   当前 UI 显示的是 SampleCalendar，解锁真实使用需要这一步。

B. 16.14 / 16.21 文档补充
   纯文档、无 ABI 影响、可随时中断。

C. Android 落盘的真机验证
   需要一个设备/模拟器。当前只有编译验证。

D. （已查证不可行）补 Android ABI 门禁
   KGP 内置工具做不到，见第五节缺口 2 的证据。
   若确实需要，唯一路径是引入 legacy BCV 单独覆盖 Android。

E. （已完成）klib 成员级 ABI
   修法：include 的 `.*` → `.**`。已用破坏性验证确认两个引擎都报警。

不推荐：iOS 落盘（Windows 上无法编译验证，只能盲写）
```

### 操作纪律（两次踩坑换来的）

```text
改 build 脚本 / 含中文的文件  →  用 write / edit 工具
  用 pwsh 字符串替换会因引号与编码问题出错（把 core/build.gradle.kts 改坏过一次）

对含中文的文件做结构检查（行号、fence 配对）  →  必须 -Encoding UTF8
  PS 5.1 的 Get-Content 默认按 ANSI 解码 UTF-8，
  会让行号错位、并得出"代码块没闭合"这类假结论（踩过一次）

改 ABI 过滤器后  →  两个引擎都要验证
  不要从"JVM 看起来正常"推断过滤器没问题

跑 --write-verification-metadata 时  →  任务集必须**足够全**
  用子集任务集写入，会把未解析到的配置条目**从文件里删掉**
  （踩过一次：只跑了 app:shared 的几个编译任务，
    结果 buildscript classpath 的 22 个 artifact 校验失败）
  症状：平时能过，configuration cache 失效后突然
        "Dependency verification failed for configuration 'classpath'"
  修法：gradlew help --write-verification-metadata sha256 先覆盖 buildscript，
        再用完整的日常验证任务集跑一遍
  教训：这个文件的"重新生成"是有损操作，改动了就要跑全量
  不要从"JVM 看起来正常"推断过滤器没问题
```

### 浏览器测试（16.35 发现）

```text
:core:jsBrowserTest / :core:wasmJsBrowserTest 是**真实浏览器**里跑的
  （Karma + ChromeHeadless154），不是模拟环境
```

首次运行会触发下载 Node 发行版，而校验门禁会拦住：

```powershell
# 锁与校验元数据必须**同一次调用里同时写**（单独写会互相阻塞）
.\gradlew.bat :kotlinNpmInstall --write-locks --write-verification-metadata sha256

# js 与 wasmJs 用**不同**的 Node 版本，两个都要单独记录一次
```

**教训**：我此前的"全量任务集"从来没包含 npm / webpack 那条流水线，
所以锁与校验元数据对它一直有缺口。标准验证命令里现在带上了
`*BrowserTest`。
## 七、环境与操作坑（踩过的）

```text
pwsh = PowerShell 5.1；GRADLE_USER_HOME=D:\.gradle
改 build 脚本/含中文的文件 → 用 write / edit 工具
  用 pwsh 字符串替换会因引号与编码出问题（踩过一次）
控制台显示中文是 mojibake，但文件本身是 UTF-8
  不要根据 Get-Content 的输出"修"中文
Kotlin 块注释可嵌套，注释里写 /* 会破坏文件
XML 注释不能含 "--"
KMP common 没有 toSortedSet / toSortedMap / KClass.members
Duration 用 3.hours，不是 3.hours()

9. 这台机器没有 pwsh（PowerShell 7），只有 powershell 5.1
   文档里写 pwsh -File 会直接"无法识别"。用 powershell -ExecutionPolicy Bypass -File。
   另一个坑：PS 5.1 读无 BOM 的 UTF-8 .ps1 会按 ANSI 解码，
   中文注释变乱码后**能破坏解析**（报 MissingEndCurlyBrace 之类）。
   所以 tools/*.ps1 必须带 UTF-8 BOM；
   edit/write 工具保存会丢掉 BOM，改完要补：
     $t=[IO.File]::ReadAllText($p,[Text.Encoding]::UTF8)
     [IO.File]::WriteAllText($p,$t,(New-Object Text.UTF8Encoding($true)))

10. jpackage 打出来的桌面应用和 :app:desktopApp:run 不是一回事
   jdeps 看不到反射，裁出来的 runtime 可能缺模块：
   症状是 exe 启动即退，stderr 报 NoClassDefFoundError: java/sql/DriverManager
   修法：nativeDistributions { modules("java.sql", "java.naming", "jdk.unsupported") }
   验证：runtime\release 里 MODULES 含 java.sql + exe 跑 25 秒还活着

11. .NET Framework 的 ZipFile::CreateFromDirectory 写**反斜杠**条目名
   Windows 解压能忍，Linux unzip 会解出一堆名字带 \ 的文件。
   发布脚本自己写条目名（正斜杠）+ 排序 + 固定 1980 时间戳，
   于是同样输入两次打包逐字节相同（VERSION.txt 除外，那是打包时间）。
```

## 八、当前运行的实例

```text
没有常驻实例（16.36 结束时已关闭桌面窗口与静态服务器）
数据库 C:\Users\moiclathy\.dism\dism.db （262,144 bytes）
  里面的 e2e-1 记录是端到端验证留下的，不是脏数据
```

## 九、规格文件位置

```text
E:\dism s\ui封装参考.txt     UI 阶段 16.0–16.18 的指导
                             （不含 16.20/16.21，那两个编号是本项目自行补充的）
```

合并工具脚本在 `%TEMP%\dism-merge\`（extract-spec.ps1 / apply.ps1 等）。
