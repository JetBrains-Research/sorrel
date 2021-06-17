package com.jetbrains.sorrel.plugin.inlays.maven

import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.xml.XmlTag
import com.jetbrains.sorrel.plugin.inlays.addLicenseNameInlineIfExists
import com.jetbrains.sorrel.plugin.utils.licenseDetectorModel

class LicenseXmlInlayHintsCollector(editor: Editor) : FactoryInlayHintsCollector(editor) {
    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        if (element.containingFile.name == "pom.xml") {

            val model = element.project.licenseDetectorModel()

            val installedPackagesInfo = model.installedPackages.value

            if (element is XmlTag) {
                val groupId: String? = element.getSubTagText("groupId")
                val artifactId: String? = element.getSubTagText("artifactId")
                if (groupId != null && artifactId != null) {
                    sink.addLicenseNameInlineIfExists(
                        groupId,
                        artifactId,
                        installedPackagesInfo,
                        element.textOffset + element.textLength,
                        editor,
                        factory
                    )
                }
            }
            return true
        }
        return false
    }
}