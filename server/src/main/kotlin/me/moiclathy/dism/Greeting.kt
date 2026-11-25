package me.moiclathy.dism

/**
 * Ktor server 的最小 greeting。
 *
 * 说明：
 *
 * 项目最初由 KMP 模板生成时，这个函数位于
 * `core/src/commonMain/kotlin/me/moiclathy/dism/GreetingUtil.disabled`。
 *
 * 该文件后来被主动 disabled（因为 core 是所有 KMP target 共用的领域模块，
 * 不应该携带模板 greeting），但 server 的 `/` 路由仍然引用它，
 * 导致 `:server:compileKotlin` 无法编译。
 *
 * 这里把 greeting 放回真正使用它的 server 模块，
 * 保持 `/` 的响应行为不变，同时让 core 保持干净。
 */
fun sayHello(to: String): String =
    "Hello, $to!"
