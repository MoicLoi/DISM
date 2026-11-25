# Contributing

## 前置

```text
JDK 11+
Kotlin 2.4.10 / AGP 9.0.1 / SQLDelight 2.3.2
```

## 开发循环

```powershell
# 1. 先让 core 编过
.\gradlew.bat :core:compileKotlinJvm --console=plain

# 2. 跑完整测试
.\gradlew.bat :core:jvmTest --console=plain

# 二进制 API 门禁
.\gradlew.bat :core:checkKotlinAbi --console=plain

# 3. 跨目标 + 下游
.\gradlew.bat :core:compileKotlinJs `
              :core:compileKotlinWasmJs `
              :core:compileAndroidMain `
              :core:compileTestKotlinJs `
              :server:compileKotlin `
              :app:shared:compileKotlinJvm --console=plain
```

## 改动前必读

- [architecture-rules.md](architecture-rules.md) —— 硬规则
- [testing.md](testing.md) —— 测试分层与常见坑

## 提交前自查

```text
□ :core:jvmTest 全绿
□ JS / Wasm / Android 主源集编译通过
□ :core:compileTestKotlinJs 编译通过
□ server / app 编译通过
□ :core:checkKotlinAbi 通过（公共 ABI 未意外变化）
□ 依赖校验与版本锁定通过
  （换过依赖就要跑 --write-verification-metadata sha256 / --write-locks）
□ 没有把 internal 类型放进公共签名
□ 新增的差异/指纹输出做了确定性排序
□ 没有"配置里有、实际没人用"的字段
```

最后一条最重要。一个什么都不做的开关比没有这个开关更糟——
用户会以为自己已经开启了某个能力。
