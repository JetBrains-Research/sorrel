package com.jetbrains.licensedetector.intellij.plugin.detection

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VfsUtilCore.iterateChildrenRecursively
import com.intellij.psi.PsiManager
import com.jetbrains.licensedetector.intellij.plugin.licenses.ALL_SUPPORTED_LICENSE
import com.jetbrains.licensedetector.intellij.plugin.licenses.License
import com.jetbrains.licensedetector.intellij.plugin.licenses.NoLicense
import com.jetbrains.licensedetector.intellij.plugin.licenses.SupportedLicense

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

    fun getPackageLicensesFromJar(library: Library, project: Project): Set<License> {
        val result: MutableSet<License> = mutableSetOf()

        val libraryRootFiles = library.getFiles(OrderRootType.CLASSES)

        for (rootFile in libraryRootFiles) {
            val rootFileChildren = rootFile.children

            for (childrenFile in rootFileChildren) {
                if (!childrenFile.isDirectory && licenseFileNamePattern.matches(childrenFile.name)) {
                    val fileText = FileDocumentManager.getInstance().getDocument(childrenFile)?.text ?: continue
                    result.add(getLicenseByFullText(fileText))
                }
            }

            val metaInfFolder = rootFile.findChild(metaInfoFolderName) ?: continue
            if (metaInfFolder.isDirectory) {
                iterateChildrenRecursively(metaInfFolder,
                    null,
                    {
                        if (licenseFileNamePattern.matches(it.name)) {
                            val fileText = FileDocumentManager.getInstance().getDocument(it)?.text
                            if (fileText != null) {
                                result.add(getLicenseByFullText(fileText))
                            }
                        }

                        if (it.name == pomXmlFileName) {
                            PsiManager.getInstance(project).findFile(it)?.accept(PomXmlPsiElementVisitor(result))
                        }
                        true
                    }
                )
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
        if (detectedLicenseByModel != NoLicense) {
            return detectedLicenseByModel
        }

        val detectedLicenseBySorensenDiesCoefficient = sorensenDiesDetector.detectLicenseByFullText(text)
        if (detectedLicenseBySorensenDiesCoefficient != null) {
            return detectedLicenseBySorensenDiesCoefficient
        }

        return NoLicense
    }
}