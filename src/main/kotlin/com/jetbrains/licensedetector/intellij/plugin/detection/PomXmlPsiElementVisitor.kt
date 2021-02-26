package com.jetbrains.licensedetector.intellij.plugin.detection

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiRecursiveElementVisitor
import com.intellij.psi.xml.XmlElementType.XML_TAG
import com.intellij.psi.xml.XmlTag
import com.jetbrains.licensedetector.intellij.plugin.detection.Detector.getLicenseOnNameOrSpdx
import com.jetbrains.licensedetector.intellij.plugin.licenses.License

class PomXmlPsiElementVisitor(private val resultLicenseSet: MutableSet<License>) : PsiRecursiveElementVisitor() {
    private val licensesTagName = "licenses"
    private val licenseTagName = "license"
    private val licenseNameTagName = "name"

    override fun visitElement(element: PsiElement?) {
        if (element is XmlTag && element.node.elementType == XML_TAG && element.name == licensesTagName) {
            val licensesSubTags = element.subTags
            for (subTag in licensesSubTags) {
                if (subTag.name == licenseTagName) {
                    val nameTag = subTag.findFirstSubTag(licenseNameTagName)
                    if (nameTag != null) {
                        val licenseName = nameTag.value.text
                        val license = getLicenseOnNameOrSpdx(licenseName)
                        if (license != null) {
                            resultLicenseSet.add(license)
                        }
                    }
                }
            }
        }

        super.visitElement(element)
    }
}