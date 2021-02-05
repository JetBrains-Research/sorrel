package com.jetbrains.licensedetector.intellij.plugin.diff

import com.intellij.diff.comparison.isAlpha
import com.intellij.diff.comparison.isContinuousScript
import com.intellij.openapi.util.text.StringUtil
import java.util.*

// Following methods are a workaround for missing methods and a private modifier

//
// Helpers
//
interface InlineChunk {
    fun getOffset1(): Int
    fun getOffset2(): Int
}

internal class WordChunk(
    private val myText: CharSequence,
    private val myOffset1: Int,
    private val myOffset2: Int,
    private val myHash: Int
) : InlineChunk {
    val content: CharSequence
        get() = myText.subSequence(myOffset1, myOffset2)

    override fun getOffset1(): Int {
        return myOffset1
    }

    override fun getOffset2(): Int {
        return myOffset2
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false
        val word = other as WordChunk
        return if (myHash != word.myHash) false else StringUtil.equals(content, word.content)
    }

    override fun hashCode(): Int {
        return myHash
    }
}

internal class NewlineChunk(private val myOffset: Int) : InlineChunk {
    override fun getOffset1(): Int {
        return myOffset
    }

    override fun getOffset2(): Int {
        return myOffset + 1
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return !(other == null || javaClass != other.javaClass)
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

fun getInlineChunks(text: CharSequence): List<InlineChunk> {
    val chunks: MutableList<InlineChunk> = ArrayList()
    val len = text.length
    var offset = 0
    var wordStart = -1
    var wordHash = 0
    while (offset < len) {
        val ch = Character.codePointAt(text, offset)
        val charCount = Character.charCount(ch)
        val isAlpha = isAlpha(ch)
        val isWordPart = isAlpha && !isContinuousScript(ch)
        if (isWordPart) {
            if (wordStart == -1) {
                wordStart = offset
                wordHash = 0
            }
            wordHash = wordHash * 31 + ch
        } else {
            if (wordStart != -1) {
                chunks.add(WordChunk(text, wordStart, offset, wordHash))
                wordStart = -1
            }
            if (isAlpha) { // continuous script
                chunks.add(WordChunk(text, offset, offset + charCount, ch))
            } else if (ch == '\n'.toInt()) {
                chunks.add(NewlineChunk(offset))
            }
        }
        offset += charCount
    }
    if (wordStart != -1) {
        chunks.add(WordChunk(text, wordStart, len, wordHash))
    }
    return chunks
}