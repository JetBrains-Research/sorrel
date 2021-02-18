import com.jetbrains.licensedetector.intellij.plugin.licenses.*
import io.kinference.data.tensors.Tensor
import io.kinference.data.tensors.asTensor
import io.kinference.model.Model
import io.kinference.ndarray.arrays.FloatNDArray
import io.kinference.ndarray.arrays.LongNDArray
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths

class Detector {
    // Base paths
    private val baseDir = Paths.get("").toAbsolutePath()
    private val resourcesDir = baseDir.resolve("src/main/resources/")

    // Paths to important files
    private val modelPath = resourcesDir.resolve("license_level_model_v1.onnx")
    private val vocabularyPath = resourcesDir.resolve("license_level_model_words.txt")
    private val classesPath = resourcesDir.resolve("license_level_classes.txt")


    // Encode/decode stuff
    private val vocabulary = mutableListOf<String>()
    private val classes = mutableListOf<String>()

    // License to License class
    private val licenseToClass = mapOf<String, License>(
        "Apache-2.0" to Apache_2_0,
        "BSD-3-Clause" to BSD_3_Clause,
        "GPL-3.0-only" to GPL_3_0_or_later,
        "LGPL-2.1-only" to LGPL_2_1_or_later,
        "MIT" to MIT
    )

    // Vocabulary for encoding text & classes for decode predictions
    init {
        val vocabularyFile = vocabularyPath.toFile()
        vocabularyFile.forEachLine { line -> vocabulary.add(line) }

        val classesFile = classesPath.toFile()
        classesFile.forEachLine { line -> classes.add(line) }
    }

    // Model & vectorizer for detection licenses on project level
    private val model = Model.load(modelPath.toString())
    private val vectorizer = Vectorizer(vocabulary)

    // Number of features that model accepts
    private val numFeatures = vectorizer.vector_dim

    // Shape of input data
    private val inputShape = listOf(numFeatures).toIntArray()

    private fun extractFiles(path: String): List<File> {
        /**
         * Given a path to some directory. Extracts all files from this directory.
         * @param path path to directory from which to extract files.
         * @return list of File detected objects.
         */
        val files = mutableListOf<File>()

        for (file in File(path).walk()) {
            if (file.isFile) {
                files.add(file)
            }
        }

        return files
    }

    private fun filterText(text: String): String {
        /**
         * Removes from text all non-alphanumeric characters and extra non-printable symbols.
         * @param text the text to be filtered.
         * @return filtered text.
         */
        val re = Regex("[^A-Za-z0-9 ]")
        val cleanText = text.replace("\\s+".toRegex(), " ")

        return re.replace(cleanText.toLowerCase(), "")
    }

    fun filterText(text: Iterable<String>): String {
        /**
         * Join iterable object of Strings into one string and removes from it
         * all non-alphanumeric characters and extra non-printable symbols.
         * @param text strings to be joined and filtered
         * @return filtered text in one string
         */

        return filterText(text.joinToString())
    }

    fun preprocessFile(path: String): String {
        return filterText(File(path).readText())
    }

    fun preprocessFile(file: File): String {
        return filterText(file.readText())
    }

    private fun findLicensesFiles(path: String): List<File> {
        val files = mutableListOf<File>()
        val regexps = mutableListOf<Regex>(Regex("LICENSE*"),
            Regex("LEGAL*"),
            Regex("COPYING*"),
            Regex("COPYLEFT*"),
            Regex("COPYRIGHT*"),
            Regex("UNLICENSE*"),
            Regex("MIT*"),
            Regex("BSD*"),
            Regex("GPL*"),
            Regex("LGPL*"))

        for (file in extractFiles(path)) {
            for (regexp in regexps) {
                if (regexp.containsMatchIn(file.name)) {
                    files.add(file)
                }
            }
        }

        return files
    }

    fun getMainLicense(path: String): File {
        val files = extractFiles(path)

        return files[0]
    }

    private fun detectLicense(path: String): License  {
        // Data preparation
        val vector = vectorizer.vectorize(File(path))
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

    fun detect(path: String): Map<String, License> {
        val fileToLicense = mutableMapOf<String, License>()

        for (file in findLicensesFiles(path)) {
            fileToLicense[file.absolutePath] = detectLicense(file.absolutePath)
        }

        return fileToLicense
    }

    fun detectProjectLicense(path: String): Map<String, License> {
        val mainLicenseFile = getMainLicense(path).absolutePath
        val retValue = mutableMapOf<String, License>()

        retValue[mainLicenseFile] = detectLicense(mainLicenseFile)
        return retValue
    }

    fun detect(path: Path): Map<String, License> {
        return detect(path.toString())
    }

}