package com.jetbrains.sorrel.plugin.utils

import com.intellij.openapi.roots.libraries.Library

fun Library.getSimpleIdentifier(): String? {
    val name: String = this.name ?: return null
    return name.substringAfter(' ').substringBeforeLast(':')
}

fun Library.getVersion(): String? {
    val name: String = this.name ?: return null
    return name.substringAfterLast(':')
}