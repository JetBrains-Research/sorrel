package com.jetbrains.sorrel.plugin.detection

import com.intellij.openapi.util.text.Strings
import com.jetbrains.sorrel.plugin.licenses.ALL_SUPPORTED_LICENSE
import com.jetbrains.sorrel.plugin.licenses.SupportedLicense

/**
 * Detector for determining the type of license by its full text using the Sorensen-Dies coefficient
 */
class SorensenDiesDetector {

    private val THRESHOLD = 0.98
    private val manySpacesRegex = Regex("\\s+")

    private val referenceLicensesTokens = ALL_SUPPORTED_LICENSE.map {
        Pair(it, tokenizeText(it.fullText))
    }

    private fun tokenizeText(text: String): Set<String> {
        return text.split(manySpacesRegex).map { Strings.toLowerCase(it) }.toSet()
    }

    fun detectLicenseByFullText(licenseText: String): SupportedLicense? {
        val tokenizedLicenseText = tokenizeText(licenseText)
        val coefficientForLicenses = referenceLicensesTokens.map {
            Pair(
                it.first, (2.0 * it.second.intersect(tokenizedLicenseText).size) /
                        (it.second.size + tokenizedLicenseText.size)
            )
        }.filter { it.second > THRESHOLD }
        return coefficientForLicenses.maxByOrNull { it.second }?.first
    }

}