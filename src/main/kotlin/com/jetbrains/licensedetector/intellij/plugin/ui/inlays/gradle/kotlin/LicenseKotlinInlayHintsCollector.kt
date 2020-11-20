package com.jetbrains.licensedetector.intellij.plugin.ui.inlays.gradle.kotlin

import com.intellij.codeInsight.hints.FactoryInlayHintsCollector
import com.intellij.codeInsight.hints.InlayHintsSink
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.jetbrains.licensedetector.intellij.plugin.ui.inlays.roundWithBackground
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.LicenseDetectorToolWindowFactory
import com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model.LicenseDetectorDependency
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.plugins.groovy.lang.psi.util.GrStringUtil

class LicensesKotlinInlayHintsCollector(editor: Editor) : FactoryInlayHintsCollector(editor) {
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

        if (element.containingFile.name == "build.gradle.kts") {
            val model = element.project.getUserData(LicenseDetectorToolWindowFactory.ToolWindowModelKey)

            if (model != null) {
                val installedPackagesInfo = model.installedPackages.value
                if (element is KtCallExpression) {

                    val elementArguments = element.valueArguments


                    if (elementArguments.size == 3) {
                        val firstArg = elementArguments[0]
                        val secondArg = elementArguments[1]
                        val thirdArg = elementArguments[2]
                        val firstArgChildren = firstArg.children
                        val secondArgChildren = secondArg.children
                        val thirdArgChildren = thirdArg.children

                        //For implementation(group = "io.ktor", name = "ktor", version = "1.4.0")
                        //Arguments can be rearranged with each other
                        if (firstArgChildren.size == 2 &&
                                secondArgChildren.size == 2 &&
                                thirdArgChildren.size == 2) {
                            var groupId = ""
                            var artifactId = ""

                            val argHandler = { arg: KtValueArgument ->

                                val argValue = arg.getArgumentExpression()?.text
                                val argName = arg.getArgumentName()?.text
                                if (argValue != null && argName != null) {
                                    when (argName) {
                                        "group" -> groupId = GrStringUtil.removeQuotes(argValue)
                                        "name" -> artifactId = GrStringUtil.removeQuotes(argValue)
                                    }
                                }
                            }

                            argHandler(firstArg)
                            argHandler(secondArg)
                            argHandler(thirdArg)

                            addLicenseNameInlineIfExists(groupId, artifactId, installedPackagesInfo)
                        }


                        // For implementation("io.ktor", "ktor", "1.4.0")
                        if (firstArgChildren.size == 1 && secondArgChildren.size == 1) {
                            val firstArgSingleChildren = firstArgChildren[0]
                            val secondArgSingleChildren = secondArgChildren[0]
                            if (firstArgSingleChildren is KtStringTemplateExpression &&
                                    secondArgSingleChildren is KtStringTemplateExpression) {
                                val groupId = GrStringUtil.removeQuotes(firstArgSingleChildren.text)
                                val artifactId = GrStringUtil.removeQuotes(secondArgSingleChildren.text)
                                addLicenseNameInlineIfExists(groupId, artifactId, installedPackagesInfo)
                            }
                        }
                    }

                    //For implementation("io.ktor:ktor:1.4.0")
                    if (elementArguments.size == 1) {
                        val argumentWithoutQuotes: String = GrStringUtil.removeQuotes(elementArguments[0].text)

                        val groupIdAndArtifactId = argumentWithoutQuotes.substringBeforeLast(":")
                        val groupId = groupIdAndArtifactId.substringBefore(":")
                        val artifactId = groupIdAndArtifactId.substringAfter(":")

                        addLicenseNameInlineIfExists(groupId, artifactId, installedPackagesInfo)
                    }

                }
                return true
            }
        }
        return false
    }
}