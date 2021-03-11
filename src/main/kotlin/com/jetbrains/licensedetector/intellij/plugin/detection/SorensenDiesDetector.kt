package com.jetbrains.licensedetector.intellij.plugin.detection

import com.jetbrains.licensedetector.intellij.plugin.licenses.ALL_SUPPORTED_LICENSE
import com.jetbrains.licensedetector.intellij.plugin.licenses.SupportedLicense

/**
 * Detector for determining the type of license by its full text using the Sorensen-Dies coefficient
 */
class SorensenDiesDetector {

    private val THRESHOLD = 0.95
    private val manySpacesRegex = Regex("\\s+")

    private val referenceLicensesTokens = ALL_SUPPORTED_LICENSE.map {
        Pair(it, tokenizeText(it.fullText))
    }

    private fun tokenizeText(text: String): Set<String> {
        return text.split(manySpacesRegex).toSet()
    }

    fun detectLicenseByFullText(licenseText: String): SupportedLicense? {
        val tokenizedLicenseText = tokenizeText(licenseText)

        return referenceLicensesTokens.find {
            val sorensenDiesCoefficient = (2.0 * it.second.intersect(tokenizedLicenseText).size) /
                    (it.second.size + tokenizedLicenseText.size)
            sorensenDiesCoefficient > THRESHOLD
        }?.first
    }

}