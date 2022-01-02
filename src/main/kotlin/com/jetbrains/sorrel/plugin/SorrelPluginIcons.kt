package com.jetbrains.sorrel.plugin

import com.intellij.openapi.util.IconLoader

object SorrelPluginIcons {
    val Artifact by lazy { IconLoader.getIcon("/icons/artifact.svg", this.javaClass) }
    val Package by lazy { IconLoader.getIcon("/icons/package.svg", this.javaClass) }
    val Export by lazy { IconLoader.getIcon("/icons/export.svg", this.javaClass) }
}
