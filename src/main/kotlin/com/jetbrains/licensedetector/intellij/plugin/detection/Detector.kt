package com.jetbrains.licensedetector.intellij.plugin.detection

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VfsUtilCore.iterateChildrenRecursively
import com.intellij.psi.PsiManager
import com.jetbrains.licensedetector.intellij.plugin.licenses.*
import io.kinference.data.map.ONNXMap
import io.kinference.data.seq.ONNXSequence
import io.kinference.data.tensors.Tensor
import io.kinference.data.tensors.asTensor
import io.kinference.model.Model
import io.kinference.ndarray.arrays.FloatNDArray
import io.kinference.ndarray.arrays.LongNDArray
import java.io.File
import java.nio.file.Path
import java.util.*

/**
 * This class provide all logics for License detection.
 */
object Detector {

    val licenseFileNamePattern: Regex = Regex(
        "(.*LICENSE.*|.*LEGAL.*|.*COPYING.*|.*COPYLEFT.*|.*COPYRIGHT.*|.*UNLICENSE.*|" +
                ".*MIT.*|.*BSD.*|.*GPL.*|.*LGPL.*|.*APACHE.*)(\\.txt|\\.md|\\.html)?", RegexOption.IGNORE_CASE
    )

    private const val metaInfoFolderName = "META-INF"
    private const val pomXmlFileName = "pom.xml"

    //Classes for decode numeric predictions
    private val classes: List<String> = Detector::class.java.getResourceAsStream(
        "/detection/license_level_classes.txt"
    ).reader().readLines()

    // Detected License (string) to License class mapping
    private val licenseToClass = mapOf<String, License>(
        "Apache-2.0" to Apache_2_0,
        "BSD-3-Clause" to BSD_2_Clause,
        "BSD-3-Clause" to BSD_3_Clause,
        "GPL-2.0-only" to GPL_2_0_only,
        "GPL-3.0-only" to GPL_3_0_only,
        "ISC" to ISC,
        "LGPL-2.1-only" to LGPL_2_1_only,
        "LGPL-3.0-only" to LGPL_3_0_only,
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
    private const val THRESHOLD = 0.8

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
        for (file in extractFiles(path)) {
            if (licenseFileNamePattern.matches(file.name)) {
                files.add(file)
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
    private fun detectLicense(text: String): License {
        // Convert text into vector
        val filteredText = filterText(text)
        val vector = vectorizer.vectorize(filteredText)
        val tensor = FloatNDArray(inputShape) { vector[it].toFloat() }.asTensor("features")

        // Prediction
        val prediction = model.predict(listOf(tensor))

        // Data transformation
        val predTensor = prediction[0] as Tensor
        val data = predTensor.data as LongNDArray
        val array = data.array.blocks

        val classIndex = array[0][0].toInt()
        val license = classes[classIndex]


        val unpack1 = ((prediction[1] as ONNXSequence).data as ArrayList<ONNXMap>)[0]
        val unpack2 = (unpack1.data as HashMap<Long, Tensor>)[classIndex.toLong()] as Tensor
        val probability = (unpack2.data as FloatNDArray).array.blocks[0][0]

        if (probability < THRESHOLD) {
            return NoLicense
        }
        return licenseToClass[license] ?: UnsupportedLicense(license, null, null, null)
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

    fun getPackageLicensesFromJar(library: Library, project: Project): Set<License> {
        val result: MutableSet<License> = mutableSetOf()

        val libraryRootFiles = library.getFiles(OrderRootType.CLASSES)

        for (rootFile in libraryRootFiles) {
            val rootFileChildren = rootFile.children

            for (childrenFile in rootFileChildren) {
                if (!childrenFile.isDirectory && licenseFileNamePattern.matches(childrenFile.name)) {
                    val fileText = FileDocumentManager.getInstance().getDocument(childrenFile)?.text ?: continue
                    result.add(detectLicense(fileText))
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
                                result.add(detectLicense(fileText))
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

    fun getLicenseByNameOrSpdx(name: String): SupportedLicense? {
        val detectedLicenseByModel = detectLicense(name)
        if (detectedLicenseByModel != NoLicense && detectedLicenseByModel is SupportedLicense) {
            return detectedLicenseByModel
        }

        val detectedLicenseByRegex = ALL_SUPPORTED_LICENSE.firstOrNull { it.nameSpdxRegex.matches(name) }
        if (detectedLicenseByRegex != null) {
            return detectedLicenseByRegex
        }

        return null
    }

    fun getLicenseByFullText(text: String): SupportedLicense? {
        val detectedLicenseByModel = detectLicense(text)
        if (detectedLicenseByModel != NoLicense && detectedLicenseByModel is SupportedLicense) {
            return detectedLicenseByModel
        }

        val detectedLicenseByRegex = ALL_SUPPORTED_LICENSE.firstOrNull { it.fullTextRegex.matches(text) }
        if (detectedLicenseByRegex != null) {
            return detectedLicenseByRegex
        }

        return null
    }
}