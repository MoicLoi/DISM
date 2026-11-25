package me.moiclathy.dism.presentation.home

/**
 * 16.4：Today 页能收到的用户动作。
 *
 * ### 为什么是 sealed interface 而不是回调集合
 *
 * 页面只描述"用户做了什么"，Presenter 决定"那意味着什么"。
 * 如果今天用三个回调参数、明天再加一个，UiState 与 UI 的
 * 契约就会在每次加功能时一起改。
 */
sealed interface HomeAction {

    /**
     * 重新读取当前状态。
     *
     * 注意：它**不携带时间**。时间由调用方在发起时决定，
     * 这样 Presenter 依旧是纯函数（见 `HomePresenter`）。
     */
    data object Refresh : HomeAction

    /**
     * 用户点了时间线上的某一条。
     */
    data class SelectItem(
        val itemId: String,
    ) : HomeAction

    /**
     * 用户处理掉了某条 Attention。
     */
    data class DismissAttention(
        val itemId: String,
    ) : HomeAction
}
