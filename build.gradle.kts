plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.ktor) apply false
}
/*
 * Final Release Gate：依赖可复现性。
 *
 * `lockAllConfigurations()` 会把解析结果固定进 `gradle.lockfile`。
 * 锁文件提交进仓库之后，CI 上的解析结果与本地一致；
 * 上游发了新版本也不会悄悄升上去。
 *
 * 放在根脚本、对所有子模块生效，是为了避免
 * "core 锁了、app 没锁"这种半截状态——
 * 那种状态下依然可能出现两次构建拿到不同依赖。
 *
 * 更新锁：.\gradlew.bat --write-locks <tasks>
 */
subprojects {
    dependencyLocking {
        lockAllConfigurations()
    }
}