package com.jetbrains.licensedetector.intellij.plugin.ui.inlays.gradle.groovy

import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.jetbrains.licensedetector.intellij.plugin.ui.inlays.roundWithBackground
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.LicenseDetectorToolWindowFactory
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorDependency
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrCallExpression
import org.jetbrains.plugins.groovy.lang.psi.util.GrStringUtil

class LicensesGroovyInlayHintsCollector(editor: Editor) : FactoryInlayHintsCollector(editor) {
    override fun collect(element: PsiElement, editor: Editor, sink: InlayHintsSink): Boolean {
        fun addLicenseNameInlineIfExists(groupId: String, artifactId: String, installedPackagesInfo: Map<String, LicenseDetectorDependency>) {
            val matchedPackageInfo = installedPackagesInfo["$groupId:$artifactId"]

            val mainLicenseName: String? = matchedPackageInfo?.remoteInfo?.licenses?.mainLicense?.name

            if (mainLicenseName != null) {
                sink.addInlineElement(
                        element.textOffset + element.textLength,
                        true,
                        factory.inset(roundWithBackground(factory.text(mainLicenseName), editor), 5, 0, 0, 0)
                )
            }
        }

        if (element.containingFile.name == "build.gradle") {
            val model = element.project.getUserData(LicenseDetectorToolWindowFactory.ToolWindowModelKey)

            if (model != null) {
                val installedPackagesInfo = model.installedPackages.value
                if (element is GrCallExpression) {

                    // For implementation group: 'io.ktor', name: 'ktor', version: '1.4.0'
                    // and implementation(group: 'io.ktor', name: 'ktor', version: '1.4.0')
                    if (element.namedArguments.size >= 2 &&
                            element.namedArguments[0].labelName == "group" &&
                            element.namedArguments[1].labelName == "name") {

                        val groupExpression = element.namedArguments[0].expression
                        val artifactExpression = element.namedArguments[1].expression

                        if (groupExpression is GrLiteral && artifactExpression is GrLiteral) {

                            val groupId = GrStringUtil.removeQuotes(groupExpression.text)
                            val artifactId = GrStringUtil.removeQuotes(artifactExpression.text)

                            addLicenseNameInlineIfExists(groupId, artifactId, installedPackagesInfo)
                        }
                    }

                    //For implementation "io.ktor:ktor:1.4.0" and implementation("io.ktor:ktor:1.4.0")
                    if (element.argumentList?.allArguments?.size == 1) {
                        val argument = element.argumentList!!.allArguments[0]

                        if (argument is GrLiteral) {
                            val argumentWithoutQuotes: String = GrStringUtil.removeQuotes(argument.text)

                            val groupIdAndArtifactId = argumentWithoutQuotes.substringBeforeLast(":")
                            val groupId = groupIdAndArtifactId.substringBefore(":")
                            val artifactId = groupIdAndArtifactId.substringAfter(":")

                            addLicenseNameInlineIfExists(groupId, artifactId, installedPackagesInfo)
                        }
                    }
                }
                return true
            }
        }
        return false
    }
}