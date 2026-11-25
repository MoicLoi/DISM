package me.moiclathy.dism.semantic.resolution

/**
 * 提及（mention）的种类。
 *
 * - NAMED: 给出了名字（充电宝、白色充电宝）
 * - REFERENCE: 指代性表达（那个、它、刚才那个）
 *
 * Context-only resolution 只允许 REFERENCE，
 * 防止陌生词靠强上下文被强行解析。
 *
 * Core 只消费这个类型，不负责识别具体自然语言表达
 * （"那个 / 它 / 上一个" 由自然语言解析层判断）。
 */
enum class EntityMentionKind {
    NAMED,
    REFERENCE,
}
