package me.moiclathy.dism.presentation.state

import me.moiclathy.dism.runtime.RuntimeSnapshot
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.model.RelationStatus

/**
 * 16.7：`RuntimeSnapshot -> SemanticStateUiState` 的纯映射。
 *
 * ### 一个刻意的现状：关系用 Uuid 显示
 *
 * ```text
 * 5f3a…  located_at  9c21…
 * ```
 *
 * 这不好看，但它**诚实**：语义层的 Entity 名字（"充电宝"、
 * "宿舍"）需要 11.7 Entity Binding 或外部观察数据才能真正解析，
 * 而演示数据里没有。
 *
 * 替代做法是编一个 `"Entity 5f3a"` 之类的标签，但那是假装
 * 我们认得它。目前的形态直接暴露了"这一层还没接上"，
 * 等 [RelationUi.subjectLabel] 真的接上名字时，
 * 这个文件之外的地方一行都不用改。
 */
fun semanticStateUiState(
    snapshot: RuntimeSnapshot,
): SemanticStateUiState {

    val relations =
        snapshot.relationStates
            .map { it.toRelationUi() }
            .sortedWith(
                compareBy(
                    { !it.isActive },
                    { it.subjectLabel },
                    { it.predicateLabel },
                    { it.objectLabel },
                ),
            )

    return SemanticStateUiState(
        entityCount = snapshot.entities.size,
        evidenceCount = snapshot.evidence.size,
        relationCount = snapshot.relationStates.size,
        transitionCount = snapshot.transitions.size,
        relations = relations,
        isLoading = false,
        isLoaded = true,
    )
}

private fun RelationState.toRelationUi(): RelationUi =
    RelationUi(
        subjectLabel = shortLabel(key.subjectId.toString()),
        predicateLabel = key.predicate.toString(),
        objectLabel = shortLabel(key.objectId.toString()),
        confidence = confidence,
        statusLabel = statusLabel(status),
        isActive = status == RelationStatus.ACTIVE,
        supportCount = supports.size,
    )

/**
 * Uuid → 前 8 位。
 *
 * 完整 Uuid 有 36 个字符，一列放不下、也没人会去读。
 * 前 8 位已经足够在同一屏里区分不同的实体。
 */
private fun shortLabel(
    value: String,
): String =
    if (value.length <= 8) value else value.take(8)

/**
 * 用 `when` 穷举而不是 `.name`：
 * 新增状态时编译器会提醒这里需要一句人话。
 */
private fun statusLabel(
    status: RelationStatus,
): String =
    when (status) {
        RelationStatus.ACTIVE -> "Active"
        RelationStatus.SUPERSEDED -> "Superseded"
        RelationStatus.INVALIDATED -> "Invalidated"
    }
