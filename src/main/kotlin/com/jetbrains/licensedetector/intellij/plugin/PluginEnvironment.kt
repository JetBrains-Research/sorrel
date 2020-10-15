package com.jetbrains.licensedetector.intellij.plugin

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.extensions.PluginId
import com.jetbrains.packagesearch.intellij.plugin.api.PackageSearchBundle

class PluginEnvironment {

    val pluginVersion
        get() = PluginManager.getPlugin(PluginId.getId("license-detector-plugin"))?.version
                ?: PackageSearchBundle.message("packagesearch.version.undefined")

    val ideVersion
        get() = ApplicationInfo.getInstance().strictVersion!!
}
