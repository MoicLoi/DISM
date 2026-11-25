# Testing

## 命令

```powershell
.\gradlew.bat :core:jvmTest --console=plain

# 二进制 API 门禁
.\gradlew.bat :core:checkKotlinAbi --console=plain

# 跨目标编译（每次改动都要跑）
.\gradlew.bat :core:compileKotlinJs `
              :core:compileKotlinWasmJs `
              :core:compileAndroidMain `
              :core:compileTestKotlinJs `
              :server:compileKotlin `
              :app:shared:compileKotlinJvm --console=plain
```

`:core:compileTestKotlinJs` 很容易被漏掉——它编译的是
**测试源集**的 JS 变体。只用 JVM 验证会漏掉
"JVM 有、common 没有"的 API（例如 `toSortedSet()`、`KClass.members`）。

## 聚合测试数

```powershell
$d = 'core\build\test-results\jvmTest'
$f = Get-ChildItem $d -Filter 'TEST-*.xml'
$t=0;$fa=0;$er=0
foreach ($x in $f) {
  [xml]$c = Get-Content $x.FullName
  $t += [int]$c.testsuite.tests; $fa += [int]$c.testsuite.failures
  $er += [int]$c.testsuite.errors
}
"suites=$($f.Count) tests=$t failures=$fa errors=$er"
```

## 测试分层

```text
commonTest    跨平台行为、契约、纯逻辑
jvmTest       真实 SQLDelight driver、文件、线程
```

真实数据库测试属于 `jvmTest`——`commonTest` 里没有 `File`。

## 契约测试怎么写才有用

```text
签名        编译期保证（写成函数值，写错就编译不过）
语义        运行时断言
```

`is Function3<*,*,*,*>` 这类断言对 suspend lambda **恒为 false**
（suspend 的是 `SuspendFunction3`），不要写。

同样不要写"永远为真"的断言（`if (true) a else b`）。

## 测试 fixture 的坑

`DynamicScheduleReplayTestData.decisionSnapshot()` 每次都会生成
**新的随机 id**（`Uuid.random()`）。断言确定性时必须钉住
同一个实例，否则比较的是两个不同的决策。
