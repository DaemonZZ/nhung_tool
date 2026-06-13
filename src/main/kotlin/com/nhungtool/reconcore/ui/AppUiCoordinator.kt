package com.nhungtool.reconcore.ui

object AppUiCoordinator {
    private var refreshAction: ((Boolean, AppScreen?) -> Unit)? = null
    private var shellRefreshAction: ((Boolean) -> Unit)? = null

    fun registerRefreshAction(action: (Boolean, AppScreen?) -> Unit) {
        refreshAction = action
    }

    fun registerShellRefreshAction(action: (Boolean) -> Unit) {
        shellRefreshAction = action
    }

    fun requestRefresh(force: Boolean = true, screen: AppScreen? = null) {
        refreshAction?.invoke(force, screen)
    }

    fun requestShellRefresh(force: Boolean = false) {
        shellRefreshAction?.invoke(force)
    }
}
