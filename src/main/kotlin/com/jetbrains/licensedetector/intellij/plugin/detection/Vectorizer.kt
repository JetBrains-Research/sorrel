package com.jetbrains.licensedetector.intellij.plugin.detection

import java.io.File
import kotlin.math.max

/**
 * Supportive class for Detector.
 * Transform text into vector of dimension = size of vocabulary size.
 * @property vocabulary vocabulary which use to vectorize text.
 */
class Vectorizer(val vocabulary: List<String>){
    val vector_dim = vocabulary.size

    /**
     * Vectorize (counting vectorization) given text into vector_dim size vector +
     * add length of text as last component of vector.
 *      * @param text text to be vectorized.
     * @return IntArray with vector_dim + 1 size (vector).
     */
    fun vectorizeWithLength(text: String): IntArray {
        val text_vector = vectorize(text).toMutableList()
        text_vector.add(text.length)
        return text_vector.toIntArray()
    }


    /**
     * Vectorize (counting vectorization) given text into vector_dim size vector.
     * @param text text to be vectorized.
     * @return IntArray with vector_dim size (vector).
     */
    fun vectorize(text: String): IntArray {
        // Initialize empty vector for text
        val text_vector = IntArray(vector_dim)
        val feature_count = HashMap<List<String>, Int>()
        val textList = text.split(" ")
        var max_feature_length = 0

        // Preprocessing: initialize empty mapping
        for (index in 0 until vector_dim) {
            val feature = vocabulary.get(index).split(" ")
            feature_count[feature] = 0
            max(max_feature_length, feature.size).also { max_feature_length = it }
        }

        // Going through all possible lengths and count features
        for (length in 1..max_feature_length - 1) {
            for (window in textList.windowed(length)) {
                if (window in feature_count.keys) {
                    feature_count[window] = feature_count.get(window)!! + 1
                }
            }
        }

        // Convert mapping count into vector
        for (index in 0 until vector_dim) {
            val feature = vocabulary.get(index).split(" ")
            text_vector[index] = feature_count.get(feature)!!
        }

        return text_vector
    }

    /**
     * Vectorize (counting vectorization) text from file into vector_dim size vector.
     * @param file file from which text to be vectorized.
     * @return IntArray with vector_dim size (vector).
     */
    fun vectorize(file: File): IntArray {
        return vectorize(file.readText())
    }

    /**
     * Detects index of given feature in vocabulary.
     * @param feature feature which index to be found.
     * @return index inside
     */
    fun getFeatureIndex(feature: String): Int {
        for (i in 1..vector_dim) {
            if (vocabulary[i].equals(feature)) {
                return i
            }
        }
        return -1
    }

    /**
     * Finds which feature is located in given index inside vocabulary.
     * @param index index of feature to be extracted.
     * @return feature which is located in given index.
     */
    fun getFeature(index: Int): String {
        return vocabulary[index]
    }
}