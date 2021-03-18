package com.jetbrains.licensedetector.intellij.plugin.utils

import java.util.concurrent.atomic.AtomicInteger

private val traceId = AtomicInteger(0)

internal data class TraceInfo(
    val source: TraceSource,
    val id: Int = traceId.incrementAndGet()
) {

    override fun toString() = "[$id, source=${source.name}]"

    enum class TraceSource {
        EMPTY_VALUE,
        INIT_MAIN_MODEL,
        PROJECT_CHANGES,
        PACKAGE_DEPENDENCY_LIST_CHANGES,
        SELECTED_MODULE_CHANGES,
        SEARCH_TERM_CHANGES,
        REQUESTS_REFRESH_CONTEXT,
        PROJECT_ROOT_CHANGES,
        NEW_MODULE_ADDED,
        MODULE_REMOVED,
        EXISTING_MODULE_RENAMED,
        VFS_CHANGES,
        STATUS_CHANGES
    }

    companion object {
        val EMPTY = TraceInfo(TraceSource.EMPTY_VALUE, -1)
    }
}
