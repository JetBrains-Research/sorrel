package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model

internal data class DataStatus(
    val isSearching: Boolean = false,
    val isRefreshingData: Boolean = false,
) {

    val isBusy = isRefreshingData || isSearching

    override fun toString() =
        "DataStatus(isBusy=$isBusy [isSearching=$isSearching, isRefreshingData=$isRefreshingData])"
}
