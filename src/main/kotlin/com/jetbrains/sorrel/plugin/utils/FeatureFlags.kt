package com.jetbrains.sorrel.plugin.utils

import com.intellij.openapi.util.registry.Registry

object FeatureFlags {

    val useDebugLogging: Boolean
        get() = Registry.`is`("sorrel.plugin.debug.logging", false)
}
