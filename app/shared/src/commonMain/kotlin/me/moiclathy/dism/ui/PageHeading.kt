package me.moiclathy.dism.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics

/**
 * 16.30：把一段文字标记为**标题**。
 *
 * ### 它改变的是什么
 *
 * 视觉上什么都不会变。变的是无障碍工具看到的结构：
 *
 * ```text
 * 没有 heading()   读屏只能逐行念，
 *                  用户要听完一整页才知道有什么
 * 有 heading()     可以"跳到下一个标题"，
 *                  在一页里快速定位
 * ```
 *
 * 这是"看不见但很实用"的一类无障碍改进：
 * 它不帮助健全用户，但对读屏用户是数量级的差别。
 *
 * ### 为什么用修饰符而不是单独的类型
 *
 * 标题本身仍是普通 `Text`（排版、换行、颜色都一样），
 * 只是多了一层语义。建一个 `HeadingText` 类型会把这些
 * 都要重新包一遍，而收益只是少写一个修饰符。
 *
 * ### 现状
 *
 * 每个页面只有**一个**标题。这在一个只有六个短页面的应用里
 * 价值有限，但它是正确结构：一旦页面变长、
 * 出现分节（例如 Semantic State 的"计数"与"关系"、
 * History 的"条目"与"截断提示"），标题导航就会立刻变得有用。
 */
fun Modifier.pageHeading(): Modifier =
    semantics {
        heading()
    }
