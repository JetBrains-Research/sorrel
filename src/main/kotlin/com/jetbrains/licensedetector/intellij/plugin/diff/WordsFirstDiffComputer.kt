package com.jetbrains.licensedetector.intellij.plugin.diff

import com.intellij.diff.comparison.*
import com.intellij.diff.comparison.iterables.DiffIterable
import com.intellij.diff.comparison.iterables.DiffIterableUtil
import com.intellij.diff.comparison.iterables.FairDiffIterable
import com.intellij.diff.fragments.LineFragment
import com.intellij.diff.tools.util.text.LineOffsets
import com.intellij.diff.tools.util.text.LineOffsetsUtil
import com.intellij.diff.util.DiffUserDataKeysEx
import com.intellij.diff.util.DiffUtil.getLines
import com.intellij.diff.util.Range
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.util.text.StringUtil
import java.util.*
import kotlin.collections.HashSet


class WordsFirstDiffComputer : DiffUserDataKeysEx.DiffComputer {
    override fun compute(
        text1: CharSequence,
        text2: CharSequence,
        policy: ComparisonPolicy,
        innerChanges: Boolean,
        indicator: ProgressIndicator
    ): List<LineFragment> {
        if (text1.isEmpty() && text2.isEmpty()) {
            return listOf()
        }

        val lineOffsets1 = LineOffsetsUtil.create(text1)
        val lineOffsets2 = LineOffsetsUtil.create(text2)

        val words1 = getInlineChunks(text1).filterIsInstance<WordChunk>()
        val words2 = getInlineChunks(text2).filterIsInstance<WordChunk>()

        val rawWordChanges = DiffIterableUtil.diff(words1, words2, indicator)
        val wordChanges =
            ChunkOptimizer.WordChunkOptimizer(words1, words2, text1, text2, rawWordChanges, indicator).build()

        val changedLines = collectChangedLines(
            text1,
            text2,
            policy,
            lineOffsets1,
            lineOffsets2,
            words1,
            words2,
            wordChanges,
            indicator
        )

        val lineFragments = convertIntoLineFragments(lineOffsets1, lineOffsets2, changedLines)

        if (!innerChanges) {
            return createInnerFragments(lineFragments, text1, text2, policy, InnerFragmentsPolicy.NONE, indicator)
        }

        return createInnerFragments(lineFragments, text1, text2, policy, InnerFragmentsPolicy.WORDS, indicator)
    }


    /**
     * Find proper 'outline' changed lines for a given list of changed words.
     *
     * Warning: A reader might want to reformulate this into a similar task of building
     * outline List<LineFragment> for a given 'whole-file' List<DiffFragment>, to avoid redundant re-computation of inner changes.
     * This task is harder than it looks and has multiple corner cases.
     * Think of deleted lines, modified '\n', three ComparisonPolicy, and variable 'empty side' positions for deleted chunks near ignorable symbols.
     */

    private fun collectChangedLines(
        text1: CharSequence,
        text2: CharSequence,
        policy: ComparisonPolicy,
        lineOffsets1: LineOffsets,
        lineOffsets2: LineOffsets,
        words1: List<WordChunk>,
        words2: List<WordChunk>,
        wordChanges: FairDiffIterable,
        indicator: ProgressIndicator
    ): DiffIterable {
        val builder = DiffIterableUtil.ChangeBuilder(lineOffsets1.lineCount, lineOffsets2.lineCount)

        // if no word changes or words1 is empty or words2 is empty then return empty result
        if (!wordChanges.changes().hasNext() || words1.isEmpty() || words2.isEmpty()) {
            return builder.finish()
        }

        val linesTexts1 = getLines(text1, lineOffsets1)
        val linesTexts2 = getLines(text2, lineOffsets2)

        /*
        Consider this case:

          a aa

          a

          aaa

        and

          a aa


          aaa

        The second text is split into two unchanged parts.
        In the second text, we move to the bottom edge by empty lines in the upper unchanged part and
         move the top edge to the empty lines in the lower unchanged part.
        As a result, we get intersections - everything breaks down.

        In order to avoid this, we store all the lines that are included
         in the artificially moved edges of unchanged blocks to avoid intersections.

        */
        val isEmptyLineProcessedAsUnchangedLines1 = HashSet<Int>()
        val isEmptyLineProcessedAsUnchangedLines2 = HashSet<Int>()


        // In the algorithm below, I work with Range as segments. That is, the right and left edge are included in the interval.

        val unchangedLines = wordChanges.iterateUnchanged().map { range ->
            val firstWord1 = words1[range.start1]
            val firstWord2 = words2[range.start2]

            val startOffsetForFirstWord1 = firstWord1.getOffset1()
            val startOffsetForFirstWord2 = firstWord2.getOffset1()

            val startNumberLineForText1 = lineOffsets1.getLineNumber(startOffsetForFirstWord1)
            val startNumberLineForText2 = lineOffsets2.getLineNumber(startOffsetForFirstWord2)


            val lastWord1 = words1[range.end1 - 1]
            val lastWord2 = words2[range.end2 - 1]

            val endOffsetForLastWord1 = lastWord1.getOffset2()
            val endOffsetForLastWord2 = lastWord2.getOffset2()

            val endNumberLineForText1 = lineOffsets1.getLineNumber(endOffsetForLastWord1)
            val endNumberLineForText2 = lineOffsets2.getLineNumber(endOffsetForLastWord2)

            for (i in startNumberLineForText1..(endNumberLineForText1 + 1)) {
                isEmptyLineProcessedAsUnchangedLines1.add(i)
            }

            for (i in startNumberLineForText2..(endNumberLineForText2 + 1)) {
                isEmptyLineProcessedAsUnchangedLines2.add(i)
            }

            return@map Range(
                startNumberLineForText1,
                endNumberLineForText1,
                startNumberLineForText2,
                endNumberLineForText2
            )
        }

        val correctedUnchangedLines = unchangedLines.map { range ->

            var startNumberLineForText1: Int = range.start1
            var startNumberLineForText2: Int = range.start2
            var endNumberLineForText1: Int = range.end1
            var endNumberLineForText2: Int = range.end2

            // Fix the case when there is an empty line before the start line
            while (startNumberLineForText1 != 0 &&
                StringUtil.isEmptyOrSpaces(linesTexts1[startNumberLineForText1 - 1]) &&
                !isEmptyLineProcessedAsUnchangedLines1.contains(startNumberLineForText1 - 1)
            ) {
                startNumberLineForText1 -= 1
                isEmptyLineProcessedAsUnchangedLines1.add(startNumberLineForText1)
            }

            while (startNumberLineForText2 != 0 &&
                StringUtil.isEmptyOrSpaces(linesTexts2[startNumberLineForText2 - 1]) &&
                !isEmptyLineProcessedAsUnchangedLines2.contains(startNumberLineForText2 - 1)
            ) {
                startNumberLineForText2 -= 1
                isEmptyLineProcessedAsUnchangedLines2.add(startNumberLineForText2)
            }
            //


            // Fix the case when there is an empty line after the end line
            while (endNumberLineForText1 != lineOffsets1.lineCount - 1 &&
                StringUtil.isEmptyOrSpaces(linesTexts1[endNumberLineForText1 + 1]) &&
                !isEmptyLineProcessedAsUnchangedLines1.contains(endNumberLineForText1 + 1)
            ) {
                endNumberLineForText1 += 1
                isEmptyLineProcessedAsUnchangedLines1.add(endNumberLineForText1)
            }

            while (endNumberLineForText2 != lineOffsets2.lineCount - 1 &&
                StringUtil.isEmptyOrSpaces(linesTexts2[endNumberLineForText2 + 1]) &&
                !isEmptyLineProcessedAsUnchangedLines2.contains(endNumberLineForText2 + 1)
            ) {
                endNumberLineForText2 += 1
                isEmptyLineProcessedAsUnchangedLines2.add(endNumberLineForText2)
            }
            //

            return@map Range(
                startNumberLineForText1,
                endNumberLineForText1,
                startNumberLineForText2,
                endNumberLineForText2
            )
        }

        val changedLines = wordChanges.iterateChanges().map { range ->

            val startNumberLineForText1: Int
            val startNumberLineForText2: Int
            val endNumberLineForText1: Int
            val endNumberLineForText2: Int

            if (range.start1 == range.end1) {
                if (range.start1 == words1.size) {
                    startNumberLineForText1 = -1
                    endNumberLineForText1 = -1
                } else {
                    val firstWord1 = words1[range.start1]
                    val startOffsetForFirstWord1 = firstWord1.getOffset1()
                    startNumberLineForText1 = lineOffsets1.getLineNumber(startOffsetForFirstWord1) //- 1

                    val lastWord1 = words1[range.start1]
                    val endOffsetForLastWord1 = lastWord1.getOffset2()
                    endNumberLineForText1 = lineOffsets1.getLineNumber(endOffsetForLastWord1)
                }
            } else {
                val firstWord1 = words1[range.start1]
                val startOffsetForFirstWord1 = firstWord1.getOffset1()
                startNumberLineForText1 = lineOffsets1.getLineNumber(startOffsetForFirstWord1) //- 1

                val lastWord1 = words1[range.end1 - 1]
                val endOffsetForLastWord1 = lastWord1.getOffset2()
                endNumberLineForText1 = lineOffsets1.getLineNumber(endOffsetForLastWord1)
            }

            if (range.start2 == range.end2) {
                if (range.start2 == words2.size) {
                    startNumberLineForText2 = -1
                    endNumberLineForText2 = -1
                } else {
                    val firstWord2 = words2[range.start2]
                    val startOffsetForFirstWord2 = firstWord2.getOffset1()
                    startNumberLineForText2 = lineOffsets2.getLineNumber(startOffsetForFirstWord2) //- 1

                    val lastWord2 = words2[range.start2]
                    val endOffsetForLastWord2 = lastWord2.getOffset2()
                    endNumberLineForText2 = lineOffsets2.getLineNumber(endOffsetForLastWord2)
                }
            } else {
                val firstWord2 = words2[range.start2]
                val startOffsetForFirstWord2 = firstWord2.getOffset1()
                startNumberLineForText2 = lineOffsets2.getLineNumber(startOffsetForFirstWord2) //- 1

                val lastWord2 = words2[range.end2 - 1]
                val endOffsetForLastWord2 = lastWord2.getOffset2()
                endNumberLineForText2 = lineOffsets2.getLineNumber(endOffsetForLastWord2)
            }

            return@map Range(
                startNumberLineForText1,
                endNumberLineForText1,
                startNumberLineForText2,
                endNumberLineForText2
            )
        }

        for (unchangedRange in correctedUnchangedLines) {
            var startLine1 = unchangedRange.start1
            var endLine1 = unchangedRange.end1
            var startLine2 = unchangedRange.start2
            var endLine2 = unchangedRange.end2

            var shouldSkip = false

            for (changedRange in changedLines) {
                if (changedRange.start1 != -1 && changedRange.end1 != -1) {
                    if (endLine1 == changedRange.start1) {
                        if (endLine1 == 0) {
                            shouldSkip = true
                            break
                        } else {
                            endLine1 -= 1
                        }
                    }

                    if (startLine1 == changedRange.end1) {
                        if (startLine1 == lineOffsets1.lineCount - 1) {
                            shouldSkip = true
                            break
                        } else {
                            startLine1 += 1
                        }
                    }
                }

                if (changedRange.start2 != 1 && changedRange.end2 != 1) {
                    if (endLine2 == changedRange.start2) {
                        if (endLine2 == 0) {
                            shouldSkip = true
                            break
                        } else {
                            endLine2 -= 1
                        }
                    }

                    if (startLine2 == changedRange.end2) {
                        if (startLine2 == lineOffsets1.lineCount - 1) {
                            shouldSkip = true
                            break
                        } else {
                            startLine2 += 1
                        }
                    }
                }
            }

            if (startLine1 > endLine1 || startLine2 > endLine2) {
                shouldSkip = true
            }

            if (!shouldSkip) {
                builder.markEqual(
                    startLine1,
                    startLine2,
                    endLine1,
                    endLine2
                )
            }
        }

        return builder.finish()
    }
}