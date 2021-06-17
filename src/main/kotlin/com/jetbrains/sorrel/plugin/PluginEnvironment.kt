package com.jetbrains.sorrel.plugin

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.extensions.PluginId

class PluginEnvironment {

    companion object {
        const val PLUGIN_ID = "com.jetbrains.sorrel.intellij.plugin"
    }

    val pluginVersion
        get() = PluginManagerCore.getPlugin(PluginId.getId("sorrel-plugin"))?.version
            ?: SorrelBundle.message("sorrel.version.undefined")

    val ideVersion
        get() = ApplicationInfo.getInstance().strictVersion
}
