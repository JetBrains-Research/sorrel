package com.jetbrains.licensedetector.intellij.plugin

import com.intellij.openapi.roots.libraries.Library

fun Library.getSimpleIdentifier(): String? {
    val name: String = this.name ?: return null
    return name.substringAfter(' ').substringBeforeLast(':')
}