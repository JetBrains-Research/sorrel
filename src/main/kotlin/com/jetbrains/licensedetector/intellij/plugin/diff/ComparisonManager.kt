package com.jetbrains.licensedetector.intellij.plugin.diff

import com.intellij.diff.comparison.*
import com.intellij.diff.comparison.iterables.DiffIterable
import com.intellij.diff.fragments.DiffFragment
import com.intellij.diff.fragments.LineFragment
import com.intellij.diff.fragments.LineFragmentImpl
import com.intellij.diff.tools.util.text.LineOffsets
import com.intellij.diff.util.Range
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.util.diff.FilesTooBigForDiffException
import java.util.*

// Following methods are a workaround for missing methods and a private modifier

internal fun convertIntoLineFragments(
    lineOffsets1: LineOffsets,
    lineOffsets2: LineOffsets,
    changes: DiffIterable
): List<LineFragment> {
    val range = Range(0, lineOffsets1.lineCount, 0, lineOffsets2.lineCount)

    return ComparisonManagerImpl.convertIntoLineFragments(range, lineOffsets1, lineOffsets2, changes)
}

internal fun createInnerFragments(
    lineFragments: List<LineFragment>,
    text1: CharSequence,
    text2: CharSequence,
    policy: ComparisonPolicy,
    fragmentsPolicy: InnerFragmentsPolicy,
    indicator: ProgressIndicator
): List<LineFragment> {
    val result: MutableList<LineFragment> = ArrayList(lineFragments.size)
    var tooBigChunksCount = 0
    for (fragment in lineFragments) {
        assert(fragment.innerFragments == null)
        try {
            // Do not try to build fine blocks after few fails
            val tryComputeDifferences = tooBigChunksCount < FilesTooBigForDiffException.MAX_BAD_LINES
            result.addAll(
                createInnerFragments(
                    fragment,
                    text1,
                    text2,
                    policy,
                    fragmentsPolicy,
                    indicator,
                    tryComputeDifferences
                )
            )
        } catch (e: DiffTooBigException) {
            result.add(fragment)
            tooBigChunksCount++
        }
    }
    return result
}

internal fun createInnerFragments(
    fragment: LineFragment,
    text1: CharSequence,
    text2: CharSequence,
    policy: ComparisonPolicy,
    fragmentsPolicy: InnerFragmentsPolicy,
    indicator: ProgressIndicator,
    tryComputeDifferences: Boolean
): Collection<LineFragment> {
    if (fragmentsPolicy == InnerFragmentsPolicy.NONE) {
        return listOf(fragment)
    }
    val subSequence1 = text1.subSequence(fragment.startOffset1, fragment.endOffset1)
    val subSequence2 = text2.subSequence(fragment.startOffset2, fragment.endOffset2)
    if (fragment.startLine1 == fragment.endLine1 ||
        fragment.startLine2 == fragment.endLine2
    ) { // Insertion / Deletion
        return if (ComparisonUtil.isEquals(subSequence1, subSequence2, policy)) {
            listOf(LineFragmentImpl(fragment, emptyList()))
        } else {
            listOf(fragment)
        }
    }
    if (!tryComputeDifferences) return listOf(fragment)
    return when (fragmentsPolicy) {
        InnerFragmentsPolicy.WORDS -> {
            createInnerWordFragments(fragment, subSequence1, subSequence2, policy, indicator)
        }
        InnerFragmentsPolicy.CHARS -> {
            createInnerCharFragments(fragment, subSequence1, subSequence2, policy, indicator)
        }
        else -> {
            throw IllegalArgumentException(fragmentsPolicy.name)
        }
    }
}

internal fun createInnerWordFragments(
    fragment: LineFragment,
    subSequence1: CharSequence,
    subSequence2: CharSequence,
    policy: ComparisonPolicy,
    indicator: ProgressIndicator
): List<LineFragment> {
    val lineBlocks = ByWord.compareAndSplit(subSequence1, subSequence2, policy, indicator)
    assert(lineBlocks.size != 0)
    val startOffset1 = fragment.startOffset1
    val startOffset2 = fragment.startOffset2
    var currentStartLine1 = fragment.startLine1
    var currentStartLine2 = fragment.startLine2
    val chunks: MutableList<LineFragment> = ArrayList()
    for (i in lineBlocks.indices) {
        val block = lineBlocks[i]
        val offsets = block.offsets

        // special case for last line to void problem with empty last line
        val currentEndLine1 = if (i != lineBlocks.size - 1) currentStartLine1 + block.newlines1 else fragment.endLine1
        val currentEndLine2 = if (i != lineBlocks.size - 1) currentStartLine2 + block.newlines2 else fragment.endLine2
        chunks.add(
            LineFragmentImpl(
                currentStartLine1, currentEndLine1, currentStartLine2, currentEndLine2,
                offsets.start1 + startOffset1, offsets.end1 + startOffset1,
                offsets.start2 + startOffset2, offsets.end2 + startOffset2,
                block.fragments
            )
        )
        currentStartLine1 = currentEndLine1
        currentStartLine2 = currentEndLine2
    }
    return chunks
}

internal fun createInnerCharFragments(
    fragment: LineFragment,
    subSequence1: CharSequence,
    subSequence2: CharSequence,
    policy: ComparisonPolicy,
    indicator: ProgressIndicator
): List<LineFragment> {
    val innerChanges = doCompareChars(subSequence1, subSequence2, policy, indicator)
    return listOf(LineFragmentImpl(fragment, innerChanges))
}

internal fun doCompareChars(
    text1: CharSequence,
    text2: CharSequence,
    policy: ComparisonPolicy,
    indicator: ProgressIndicator
): List<DiffFragment> {
    val iterable: DiffIterable = when (policy) {
        ComparisonPolicy.DEFAULT -> {
            ByChar.compareTwoStep(text1, text2, indicator)
        }
        ComparisonPolicy.TRIM_WHITESPACES -> {
            ByChar.compareTrimWhitespaces(text1, text2, indicator)
        }
        else -> {
            ByChar.compareIgnoreWhitespaces(text1, text2, indicator)
        }
    }
    return ComparisonManagerImpl.convertIntoDiffFragments(iterable)
}