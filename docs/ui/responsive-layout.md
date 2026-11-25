# 响应式布局（16.33）

## 尺寸等级

与 Material 3 的 window size class 对齐：

```text
COMPACT    < 600dp     手机竖屏、窄窗口
MEDIUM     600–839dp   平板竖屏、桌面窄窗口
EXPANDED   >= 840dp    平板横屏、桌面常规窗口
```

用 Material 的档位而不是自定的阈值，因为**它们不是随意的数字**：
边界来自大量设备实测，而且与 Compose 生态里其它组件的预期一致。

## 为什么断点必须是纯函数

之前这个判断是内联的：

```kotlin
val wide = maxWidth >= 600.dp
```

一行 `>=` 看起来不值得抽出来，但它有两个问题：

```text
1. 它是**产品规则**（"多宽算宽"），埋在布局代码里
   没人会去检查它是否仍然合理
2. 它不可测——Composable 里的分支只能靠 UI 测试覆盖，
   而那需要一整套新基建
```

抽成 `(Int) -> DismWindowSize` 之后，边界值可以逐个断言。

### 边界是断点最容易出错的地方

```kotlin
assertEquals(COMPACT,  windowSizeFor(599))
assertEquals(MEDIUM,   windowSizeFor(600))   // 600 属于 MEDIUM（含）
assertEquals(MEDIUM,   windowSizeFor(839))
assertEquals(EXPANDED, windowSizeFor(840))
```

600 到底属于窄还是宽？这类问题写代码时不会想，
出问题时也不会有人怀疑到那一行。

`windowSizeFor` 收 `Int` 而不是 `Dp`：`Dp` 是 Compose 类型，
收 `Int` 让这个函数不需要任何 Compose 依赖，能在 commonTest 里跑。

## 导航规则也是纯函数

```kotlin
internal fun navDestinationsFor(size: DismWindowSize): List<DismDestination>
```

规则：

```text
implemented = false   任何尺寸下都不出现
primary               始终出现
desktopOnly           COMPACT 下不出现
```

### 这条规则修掉了一个真实的 bug

在此之前，"有没有页面"是靠一句硬编码的：

```kotlin
.filter { it != DismDestination.DIAGNOSTICS }
```

而 **MEMORY 漏掉了**：它 `primary = true`，所以**一直出现在导航里**，
点进去是一条"This screen is not implemented yet"。

更糟的是 `DismApp` 的注释里写着"Memory 不在导航里"——
**文档与行为不一致，而且没人发现**。

把"有没有页面"变成 `DismDestination` 的字段之后：

```kotlin
val primary: Boolean        // 它值不值得占一个主位置
val implemented: Boolean    // 现在能不能打开
```

这两件事**必须分开表达**：

```text
混为一谈时  要么漏列（页面做好却忘了加）
            要么列了打不开（MEMORY 之前就是这个状态）
```

导航只列 `implemented` 的。新增目的地时**必须**显式声明这一位——
不能靠忘记而蒙混过去。

### 变宽只应该增加入口

```kotlin
assertTrue(medium.containsAll(compact))
```

拉宽窗口时丢掉一个页面是很容易发生的错误（比如两个 filter 的
条件写反），而它只在特定的窗口宽度下才显示出来。

## 内容宽度上限

```kotlin
internal val MAX_CONTENT_WIDTH_DP = 840
```

### 为什么宽屏要限制内容宽度

不限制的话桌面全屏时一行会横跨 2000px 以上：

```text
09:00  实验动物学  ·  实验楼201  ·  Upcoming
       ←────── 视线要走很远才到行尾 ──────→
```

行太长会让人在换行时**丢行**（读下一行开头时找不到位置）。
舒适的正文行宽大约 45–75 个字符，对应 600–840dp。

所以内容区封顶 840dp，超出部分留白居中。

### 这与"窗口宽就多放内容"是两回事

```text
该随窗口变宽的是   导航形态（底栏 → 导轨）
不该变宽的是       单栏文字的宽度
```

上限刻意等于 EXPANDED 的断点，而不是另取一个数字——
"什么时候算宽"与"内容最多多宽"用同一个数量级更容易理解。

COMPACT 下不加这一层包装：手机宽度本来就远小于上限，
多包一层布局只会让层级更难读。

## 页面与包装分开

```kotlin
Content(...)        // 放在什么宽度里
ScreenContent(...)  // 放哪一页
```

两者混在一起时，加一个页面就要同时想清楚包装层——那是无关的细节。

## 仍然没有做到的

```text
两栏布局        没有详情页，所以"列表 + 详情"还不成立
    等某一天有了条目详情，EXPANDED 下应该是两栏：
    左边列表、右边详情，而不是把详情做成新页面

横屏手机        目前按宽度判断，横屏手机会落到 MEDIUM
    这通常是对的（横屏确实更宽），但值得真机确认

真机验证        所有断点行为只在逻辑层验证过，
    没有在实际的窄/宽/超宽窗口里看过
```

最后一条与 Android / Web 侧"仅编译验证"是同一个限制：
**我能验证规则，不能验证它在真实窗口里的样子。**
