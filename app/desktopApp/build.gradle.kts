import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

dependencies {
    implementation(project(":app:shared"))

    implementation(compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutinesSwing)

    implementation(libs.compose.uiToolingPreview)
}

compose.desktop {
    application {
        mainClass = "me.moiclathy.dism.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "me.moiclathy.dism"

            /*
             * 这三行是"打包之后真的能跑"的前提，不是可选项。
             *
             * jpackage 用 jdeps 猜运行时需要哪些 JDK 模块，而 jdeps 看不到反射：
             * SQLDelight 的 JdbcSqliteDriver 通过 java.sql.DriverManager 建连接，
             * 那是运行到才加载的。默认打出来的 runtime 里没有 java.sql，
             * 于是安装包能装、能启动、窗口还没画完就
             * NoClassDefFoundError: java/sql/DriverManager 退出。
             *
             * 用 `:app:desktopApp:run` 是发现不了的——那条路径用的是完整 JDK。
             * 只有跑 createDistributable 出来的那份才会暴露。
             *
             * jdk.unsupported   Skiko / Compose 用到 sun.misc.Unsafe
             * java.naming       JDBC 4 驱动注册路径会碰 JNDI
             */
            modules("java.sql", "java.naming", "jdk.unsupported")

            // 与 :core 的公共 API 版本（DismApiVersion.STRING）保持一致。
            // 发布版本号与 API 版本号脱节时，装的是"1.0.0"、代码里 API 报"1.5.0"，
            // 用户无法判断自己手上的到底是哪个版本。
            packageVersion = "1.5.0"
        }
    }
}