package me.moiclathy.dism.calendar

import java.io.File

/**
 * 缓存用户导入的 ICS 文本。
 *
 * ### 为什么是应用在缓存，而不是 DISM 在持久化
 *
 * DISM 目前**没有**"持久化 schedule model"的公共 API——
 * 落盘的只有决策历史（Snapshot / Trace / ReplayInput）。
 *
 * 所以重启后模型从哪来？两个选择：
 *
 * ```text
 * A. 加一个 API 让 DISM 存模型    工作量大，且要先设计模型生命周期
 * B. 应用把用户导入的 ICS 存起来   最小、且语义诚实
 * ```
 *
 * 选 B。理由不只是省事：
 *
 * ```text
 * ICS 是**用户的原始输入**，不是 DISM 的状态
 * ```
 *
 * 存原始输入、每次启动重新导入，比存一份 DISM 内部模型更不容易
 * 出现"库里那份和代码里的解析规则不一致"的问题——
 * 解析规则升级后，重新导入自动得到新结果；
 * 而存下来的旧模型会一直停留在旧规则上。
 *
 * ### 它不是数据库
 *
 * 这个类只做一件事：读写一个文本文件。
 * DISM 自己的数据在 `dism.db` 里，由 SQLDelight 管。
 */
class ImportedCalendarStore(
    private val file: File,
) : CalendarTextStore {

    /**
     * 读取缓存的 ICS 文本。
     *
     * 不存在或读不出来时返回 null——缓存丢失不该让应用起不来。
     */
    override fun load(): String? =
        try {
            if (file.exists() && file.isFile) {
                file.readText().takeIf { it.isNotBlank() }
            } else {
                null
            }
        } catch (_: Exception) {
            /*
             * 读缓存失败（权限、磁盘、编码）是可恢复的：
             * 退回"没有导入过"即可，用户重新导入一次就好。
             */
            null
        }

    /**
     * 写入缓存的 ICS 文本。
     *
     * 父目录会自动创建：首次运行时 `~/.dism/` 往往还不存在。
     *
     * ### 写入失败向上抛
     *
     * 与 [load] 不同，保存失败**必须**让调用方知道。
     *
     * ```text
     * load 失败  →  用户看到的是"没导入过"，符合事实
     * save 失败  →  用户看到"导入成功"，重启后数据没了 ← 骗人
     * ```
     *
     * 所以这里不吞异常。
     */
    override fun save(icsText: String) {
        file.parentFile?.mkdirs()

        file.writeText(icsText)
    }

    fun clear() {
        if (file.exists()) {
            file.delete()
        }
    }
}
