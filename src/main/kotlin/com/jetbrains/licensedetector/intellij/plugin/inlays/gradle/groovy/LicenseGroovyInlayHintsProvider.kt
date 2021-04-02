package com.jetbrains.licensedetector.intellij.plugin.inlays.gradle.groovy

import com.intellij.codeInsight.hints.*
import com.intellij.lang.Language
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import com.intellij.ui.layout.panel
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import org.jetbrains.plugins.groovy.GroovyLanguage
import javax.swing.JComponent

class LicenseGroovyInlayHintsProvider : InlayHintsProvider<NoSettings> {

    companion object {
        val ourKey: SettingsKey<NoSettings> = SettingsKey("licenses.groovy.hints")
    }

    override val isVisibleInSettings: Boolean
        get() = super.isVisibleInSettings
    override val key: SettingsKey<NoSettings>
        get() = ourKey
    override val name: String = LicenseDetectorBundle.message("licensedetector.ui.inlay.setting.name")
    override val previewText: String? = null

    override fun createConfigurable(settings: NoSettings): ImmediateConfigurable = object : ImmediateConfigurable {
        override fun createComponent(listener: ChangeListener): JComponent = panel {}
    }

    override fun createSettings(): NoSettings = NoSettings()

    override fun getCollectorFor(file: PsiFile, editor: Editor, settings: NoSettings, sink: InlayHintsSink): InlayHintsCollector? {
        return LicensesGroovyInlayHintsCollector(editor)
    }

    override fun isLanguageSupported(language: Language): Boolean {
        return language == GroovyLanguage
    }
}
