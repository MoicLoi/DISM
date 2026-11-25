plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
}

group = "me.moiclathy.dism"
// 与 :core 的公共 API 版本（DismApiVersion.STRING）保持一致。
version = "1.5.0"
application {
    mainClass = "me.moiclathy.dism.ApplicationKt"
}

dependencies {
    api(project(":core"))
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
}