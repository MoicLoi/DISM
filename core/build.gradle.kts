import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.sqldelight)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    /*
     * 15.14 / 15.15：Public API 的**二进制**兼容性门禁。
     *
     * 这里用的是 **Kotlin Gradle Plugin 内置**的 ABI validation，
     * 而不是 `kotlinx-binary-compatibility-validator`：
     *
     * ```text
     * BCV README（0.18.x）:
     *   "The implementation of new features has been stopped in
     *    favor of adding them to the experimental binary
     *    compatibility validation in the Kotlin Gradle plugin."
     * ```
     *
     * 换句话说 BCV 已经进入只修关键 bug 的维护状态，
     * 新能力都在 KGP 里。本项目 Kotlin 2.4.10 自带它，
     * 因此**不需要**新增任何第三方插件依赖。
     *
     * 任务：
     *
     * ```text
     * :core:updateKotlinAbi   生成/更新 api 目录下的 .api 基线
     * :core:checkKotlinAbi    校验当前二进制 API 是否与基线一致
     * ```
     *
     * 注意上面这行不能写成 `api` 加斜杠加星号的形式：
     * Kotlin 的块注释是**可嵌套**的，那个字符组合会在这里
     * 开启一个永远不闭合的嵌套注释，直接让整个 build 脚本
     * 报 "Expecting '}'"。
     *
     * 基线文件提交进仓库，`checkKotlinAbi` 接入 CI 之后，
     * "偷偷删掉一个公共方法"就会变成构建失败。
     */
    @OptIn(ExperimentalAbiValidation::class)
    abiValidation {
        /*
         * Kotlin 2.4.10 起 `enabled` 已被移除：
         * **只要调用 `abiValidation { }` 本身就是启用**。
         *
         * ### 为什么必须有 filters
         *
         * 本模块的"内部类型"只是被 `@DismInternalApi` **标记**，
         * 在 Kotlin 可见性上仍然是 `public`（15.2 的既定策略：
         * 先标记边界，不急着把 990 个文件改成 internal）。
         *
         * 不加过滤的话，dump 会把这些实现类全部算成公共 ABI：
         *
         * ```text
         * 任何内部重构  →  checkKotlinAbi 失败
         * 于是所有人开始盲跑 updateKotlinAbi  →  门禁形同虚设
         * ```
         *
         * `annotatedWith` 正好解决这件事：
         * 被这个注解标注的声明不进入 dump。
         *
         * 这样门禁覆盖的就**恰好是** 15.x 冻结的那份白名单，
         * 而不是把"实现细节的变动"也算成破坏性变更。
         *
         * （Kotlin 文档里这个选项早期叫 `nonPublicMarkers`，
         * 2.4.10 的实际 API 是 `AbiFilterSetSpec.annotatedWith`。
         * 用 `javap` 查 kotlin-gradle-plugin-api 的 jar 可以确认。）
         */
        filters {
            /*
             * 只 dump 15.x 冻结的那九个包。
             *
             * 为什么必须是 include-only：本模块有 ~990 个
             * `public` 类型，其中绝大多数（schedule 的 355 个、
             * external 的 225 个、context 的 90 个……）是
             * "约定上内部"的，并没有逐个标注 `@DismInternalApi`。
             *
             * 不加 include 的话，dump 会是 1.4MB(JVM)+3.1MB(klib)，
             * 任何内部重构都会让 checkKotlinAbi 失败。
             */
            /*
             * 注意这里必须是 `.**` 而不是 `.*`。
             *
             * `byNames` 的通配符语义（官方文档）：
             *
             * ```text
             * *    匹配零或多个字符，**不含点**（用于指定单个类名）
             * **   匹配零或多个字符，**含点**
             * ```
             *
             * 写成 `me.moiclathy.dism.api.*` 时：
             *
             * ```text
             * …api.DismCapabilities          ✓ 匹配（* 正好等于一个类名）
             * …api.DismCapabilities.hashCode ✗ 不匹配（那个点跨不过去）
             * ```
             *
             * 结果就是 dump 里**只有类型、没有成员**——
             * 而 klib 引擎对"成员没被匹配到"是直接丢弃，
             * JVM 引擎却会保留已匹配类型的成员。
             * 这个不对称曾经让 klib 基线退化成 173 行的类型清单。
             *
             * 用 `.**` 后两个引擎都覆盖到成员级。
             * （这九个包都没有子包，所以 `**` 不会顺带拉进别的东西。）
             */
            include {
                byNames.addAll(
                    "me.moiclathy.dism.api.**",
                    "me.moiclathy.dism.runtime.**",
                    "me.moiclathy.dism.config.**",
                    "me.moiclathy.dism.diagnostics.**",
                    "me.moiclathy.dism.provider.**",
                    "me.moiclathy.dism.platform.**",
                    "me.moiclathy.dism.example.**",
                    "me.moiclathy.dism.schedule.api.**",
                    "me.moiclathy.dism.schedule.model.**",
                )
            }

            /*
             * 白名单内被显式标记为 internal 的仍然排除，
             * 例如 `me.moiclathy.dism.runtime.DismRuntime`。
             */
            exclude {
                annotatedWith.add(
                    "me.moiclathy.dism.api.DismInternalApi",
                )
            }
        }
    }

    /*
     * 15.2：模块内部自我 opt-in `@DismInternalApi`。
     *
     * 这一层的目的是给**模块外**的消费者一个信号：
     *
     * ```text
     * :core    → 自己知道自己在用自己的内部 API（静默）
     * :server  → 一旦碰内部 API 就会收到 warning
     * :app     → 同上
     * ```
     *
     * 不加这个 flag 的话，:core 自己会产生 300+ 条同样内容的
     * warning，最后所有人都学会忽略它——那时这个标记就
     * 不再是信号，只是噪声。
     */
    compilerOptions {
        optIn.add("me.moiclathy.dism.api.DismInternalApi")
    }

    iosArm64()
    iosSimulatorArm64()

    jvm()

    js {
        browser()
    }

    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser()
    }

    android {
        namespace = "me.moiclathy.dism.core"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        androidResources {
            enable = true
        }
        withHostTest {
            isIncludeAndroidResources = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.sqldelight.runtime)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.serialization.json)

            /*
             * 13.14：批量 Replay 需要 Semaphore。
             *
             * 它本来就能通过 SQLDelight 传递进来，
             * 但依赖一个自己不声明的东西是脆的——
             * 上游换实现就会突然编译不过。
             */
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutinesTest)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
        }

        /*
         * 16.34：Web 的键值存储绑定。
         *
         * `kotlin-browser`（wrappers）只有 js 变体，
         * 而 webMain 同时被 js 与 wasmJs 编译；
         * `kotlinx-browser` 两个变体都有。
         *
         * 它已经在锁与校验元数据里（app:shared 的 webMain
         * 早就声明了它），所以这里新增不触发锁/元数据变更。
         */
        webMain.dependencies {
            implementation(libs.kotlinx.browser)
        }
    }
}

sqldelight {
    databases {
        create("DismDatabase") {
            packageName.set(
                "me.moiclathy.dism.persistence.db"
            )
        }
    }
}
