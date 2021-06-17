package com.jetbrains.sorrel.plugin.diff

import com.intellij.diff.comparison.ComparisonPolicy
import com.intellij.diff.comparison.trimEnd
import com.intellij.diff.comparison.trimStart
import com.intellij.openapi.util.text.StringUtil

// Following methods are a workaround for missing methods and a private modifier


internal class Line(val content: CharSequence, private val myPolicy: ComparisonPolicy) {
    private val myHash: Int = hashCode(content, myPolicy)
    val nonSpaceChars: Int = countNonSpaceChars(content)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val line = other as Line
        assert(myPolicy == line.myPolicy)
        return if (hashCode() != line.hashCode()) false else equals(content, line.content, myPolicy)
    }

    override fun hashCode(): Int {
        return myHash
    }

    companion object {
        private fun countNonSpaceChars(text: CharSequence): Int {
            var nonSpace = 0
            val len = text.length
            var offset = 0
            while (offset < len) {
                val c = text[offset]
                if (!StringUtil.isWhiteSpace(c)) nonSpace++
                offset++
            }
            return nonSpace
        }

        private fun equals(text1: CharSequence, text2: CharSequence, policy: ComparisonPolicy): Boolean {
            return when (policy) {
                ComparisonPolicy.DEFAULT -> StringUtil.equals(text1, text2)
                ComparisonPolicy.TRIM_WHITESPACES -> StringUtil.equalsTrimWhitespaces(text1, text2)
                ComparisonPolicy.IGNORE_WHITESPACES -> StringUtil.equalsIgnoreWhitespaces(text1, text2)
                else -> throw IllegalArgumentException(policy.toString())
            }
        }

        private fun hashCode(text: CharSequence, policy: ComparisonPolicy): Int {
            return when (policy) {
                ComparisonPolicy.DEFAULT -> StringUtil.stringHashCode(text)
                ComparisonPolicy.TRIM_WHITESPACES -> {
                    val offset1 = trimStart(text, 0, text.length)
                    val offset2 = trimEnd(text, offset1, text.length)
                    StringUtil.stringHashCode(text, offset1, offset2)
                }
                ComparisonPolicy.IGNORE_WHITESPACES -> StringUtil.stringHashCodeIgnoreWhitespaces(text)
                else -> throw IllegalArgumentException(policy.name)
            }
        }
    }
}