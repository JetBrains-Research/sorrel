package com.jetbrains.licensedetector.intellij.plugin.detection

import com.jetbrains.licensedetector.intellij.plugin.licenses.*
import io.kinference.data.tensors.Tensor
import io.kinference.data.tensors.asTensor
import io.kinference.model.Model
import io.kinference.ndarray.arrays.FloatNDArray
import io.kinference.ndarray.arrays.LongNDArray
import java.io.File
import java.nio.file.Path

/**
 * This class provide all logics for License detection.
 */
class Detector {
    //Classes for decode numeric predictions
    private val classes: List<String> = Detector::class.java.getResourceAsStream(
        "/detection/license_level_classes.txt"
    ).reader().readLines()

    // Detected License (string) to License class mapping
    private val licenseToClass = mapOf<String, License>(
        "Apache-2.0" to Apache_2_0,
        "BSD-3-Clause" to BSD_3_Clause,
        "GPL-3.0-only" to GPL_3_0_or_later,
        "LGPL-2.1-only" to LGPL_2_1_or_later,
        "MIT" to MIT
    )

    // Model & vectorizer for detection licenses on project level initializiation
    private val model: Model = Model.load(
        Detector::class.java.getResource("/detection/license_level_model_v1.onnx").readBytes()
    )
    private val vectorizer: Vectorizer = Vectorizer(
        Detector::class.java.getResourceAsStream(
            "/detection/license_level_model_words.txt"
        ).reader().readLines()
    )

    // Number of features that model accepts
    private val numFeatures = vectorizer.vector_dim

    // Shape of input data
    private val inputShape = listOf(numFeatures).toIntArray()

    /**
     * Given a path to some directory. Extracts all files from this directory.
     * @param path path to directory from which to extract files.
     * @return list of Files detected objects.
     */
    private fun extractFiles(path: String): List<File> {
        val files = mutableListOf<File>()

        for (file in File(path).walk()) {
            if (file.isFile) {
                files.add(file)
            }
        }

        return files
    }

    /**
     * Removes all non-alphanumeric characters and extra non-printable symbols from text.
     * Also transforms characters to lowercase.
     * @param text the text to be filtered.
     * @return filtered text.
     */
    private fun filterText(text: String): String {
        val re = Regex("[^A-Za-z0-9 ]")
        val cleanText = text.replace("\\s+".toRegex(), " ")

        return re.replace(cleanText.toLowerCase(), "")
    }

    /**
     * Removes all non-alphanumeric characters and extra non-printable symbols from text of file.
     * Also transforms characters to lowercase.
     * @param file file from which to filter text.
     * @return filtered text.
     */
    private fun filterText(file: File): String {
        return filterText(file.readText())
    }

    /**
     * Removes all non-alphanumeric characters and extra non-printable symbols
     * from text of file located at path. Also transforms characters to lowercase.
     * @param path path to file from which to filter text.
     * @return filtered text.
     */
    private fun filterText(path: Path): String {
        return filterText(path.toFile())
    }

    /**
     * Join iterable object of Strings into one string and removes from it
     * all non-alphanumeric characters and extra non-printable symbols.
     * Also transforms characters to lowercase.
     * @param strings strings to be joined and filtered
     * @return filtered text in one string
     */
    private fun filterText(strings: Iterable<String>): String {
        return filterText(strings.joinToString(separator = " "))
    }


    /**
     * Find all files which names are license-like in given path. Traverse path in-depth.
     * @param path path from which to start searching licenses files.
     * @return list of all found files which name are license-like.
     */
    private fun findLicensesFiles(path: String): List<File> {
        val files = mutableListOf<File>()
        val licensePatterns = listOf(
            Regex("LICENSE*"),
            Regex("LEGAL*"),
            Regex("COPYING*"),
            Regex("COPYLEFT*"),
            Regex("COPYRIGHT*"),
            Regex("UNLICENSE*"),
            Regex("MIT*"),
            Regex("BSD*"),
            Regex("GPL*"),
            Regex("LGPL*")
        )

        for (file in extractFiles(path)) {
            for (regexp in licensePatterns) {
                if (regexp.containsMatchIn(file.name)) {
                    files.add(file)
                }
            }
        }

        return files
    }

    /**
     * Find license-like file which is closest to path in terms of depth.
     * @param path path to which closest file to found.
     * @return file which is closest to path.
     */
    private fun getMainLicenseFile(path: String): File {
        val files = extractFiles(path)
        return files[0]
    }

    /**
     * From given text detects license class.
     * @param text text from which license to be detected.
     * @return object of detected License.
     */
    fun detectLicense(text: String): License {
        // Convert text into vector
        val filteredText = filterText(text)
        val vector = vectorizer.vectorize(filteredText)
        val tensor = FloatNDArray(inputShape) { it -> vector[it].toFloat() }.asTensor("features")

        // Prediction
        val prediction = model.predict(listOf(tensor))

        // Data transformation
        val predTensor = prediction[0] as Tensor
        val data = predTensor.data as LongNDArray
        val array = data.array.blocks

        val classIndex = array[0][0].toInt()
        val license = classes[classIndex]

        return licenseToClass[license]!!
    }

    /**
     * From given path detects license class.
     * @param path path to a file from which license to be detected.
     * @return object of detected License.
     */
    fun detectLicense(path: Path): License {
        return detectLicense(path.toFile().readText())
    }

    /**
     * From given file detects license class.
     * @param file file from which license to be detected.
     * @return object of detected License.
     */
    fun detectLicense(file: File): License {
        return detectLicense(file.readText())
    }

    /**
     * Detect license for all files which name is license-like inside project path.
     * Detection is running in-depth.
     * @param path path to the project.
     * @return mapping file path to license detected in file at path.
     */
    fun detectLicensesInProject(path: String): Map<String, License> {
        val fileToLicense = mutableMapOf<String, License>()

        for (file in findLicensesFiles(path)) {
            fileToLicense[file.absolutePath] = detectLicense(file)
        }

        return fileToLicense
    }

    /**
     * From given path to project find the file closest to root level of project.
     * And detects license in this file. (Project license)
     * @param path path to project where to detect Project license.
     * @return  object of detected License.
     */
    fun detectProjectLicense(path: String): License {
        val mainLicenseFile = getMainLicenseFile(path)
        return detectLicense(mainLicenseFile)
    }

}