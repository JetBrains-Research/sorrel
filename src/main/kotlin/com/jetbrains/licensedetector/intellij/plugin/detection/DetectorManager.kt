package com.jetbrains.licensedetector.intellij.plugin.detection

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.jetbrains.licensedetector.intellij.plugin.licenses.*
import kotlinx.coroutines.yield
import org.jetbrains.kotlin.backend.common.pop

/**
 * This class provide all logics for License detection.
 */
object DetectorManager {

    private val mlDetector = MLDetector()
    private val sorensenDiesDetector = SorensenDiesDetector()

    val licenseFileNamePattern: Regex = Regex(
        "(.*LICENSE.*|.*LEGAL.*|.*COPYING.*|.*COPYLEFT.*|.*COPYRIGHT.*|.*UNLICENSE.*|" +
                ".*MIT.*|.*BSD.*|.*GPL.*|.*LGPL.*|.*APACHE.*)(\\.txt|\\.md|\\.html)?", RegexOption.IGNORE_CASE
    )

    private const val metaInfoFolderName = "META-INF"
    private const val pomXmlFileName = "pom.xml"

    suspend fun getPackageLicensesFromJar(library: Library, project: Project): Set<License> {
        val result: MutableSet<License> = mutableSetOf()

        val libraryRootFiles = library.getFiles(OrderRootType.CLASSES)

        for (rootFile in libraryRootFiles) {
            val rootFileChildren = rootFile.children

            yield()

            for (childrenFile in rootFileChildren) {
                yield()

                if (!childrenFile.isDirectory && licenseFileNamePattern.matches(childrenFile.name)) {
                    val fileText = ReadAction.compute<Document?, Throwable> {
                        FileDocumentManager.getInstance().getDocument(childrenFile)
                    }?.text ?: continue
                    result.add(getLicenseByFullText(fileText))
                }
            }

            val metaInfFolder = rootFile.findChild(metaInfoFolderName) ?: continue
            if (metaInfFolder.isDirectory) {
                val childListToProcess = mutableListOf<VirtualFile>(*metaInfFolder.children)
                while (childListToProcess.isNotEmpty()) {
                    yield()
                    val child = childListToProcess.pop()
                    if (child.isValid) {
                        if (!child.isDirectory) {
                            if (licenseFileNamePattern.matches(child.name)) {
                                val fileText = ReadAction.compute<Document, Throwable> {
                                    FileDocumentManager.getInstance().getDocument(child)
                                }?.text
                                if (fileText != null) {
                                    result.add(getLicenseByFullText(fileText))
                                }
                            }

                            yield()

                            if (child.name == pomXmlFileName) {
                                PsiManager.getInstance(project).findFile(child)?.accept(PomXmlPsiElementVisitor(result))
                            }
                        }
                    } else {
                        childListToProcess.addAll(child.children)
                    }
                }
            }
        }
        return result.filter { it != NoLicense }.toSet()
    }

    fun getLicenseByNameOrSpdx(name: String): License {
        val detectedLicenseByModel = mlDetector.detectLicenseByShortName(name)
        if (detectedLicenseByModel != NoLicense && detectedLicenseByModel is SupportedLicense) {
            return detectedLicenseByModel
        }

        val detectedLicenseByRegex = ALL_SUPPORTED_LICENSE.firstOrNull { it.nameSpdxRegex.matches(name) }
        if (detectedLicenseByRegex != null) {
            return detectedLicenseByRegex
        }

        return detectedLicenseByModel
    }

    fun getLicenseByFullText(text: String): SupportedLicense {
        val detectedLicenseByModel = mlDetector.detectLicenseByFullText(text)
        val detectedLicenseBySorensenDiesCoefficient = sorensenDiesDetector.detectLicenseByFullText(text)

        // Handling the case when the original license is GPL-2.0-with-classpath-exception
        // and the ml model recognizes it as GPL-2.0-only.
        // In such a situation, priority should be given to the Sorensen Dies detector.
        if (detectedLicenseByModel == GPL_2_0_only && detectedLicenseBySorensenDiesCoefficient != null) {
            return detectedLicenseBySorensenDiesCoefficient
        }

        if (detectedLicenseByModel != NoLicense) {
            return detectedLicenseByModel
        }

        if (detectedLicenseBySorensenDiesCoefficient != null) {
            return detectedLicenseBySorensenDiesCoefficient
        }

        return NoLicense
    }
}