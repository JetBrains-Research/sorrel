import java.io.File
import java.nio.file.Path

class Detector {
    val vocabulary_path = "license_level_model_words.txt"
    val vocabulary = mutableListOf<String>()

    init {
        // Initialize vectorizer
        val vocabularyFile = File(vocabulary_path)
        vocabularyFile.forEachLine { line -> vocabulary.add(line) }
    }

    val vectorizer = Vectorizer(vocabulary)

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

    private fun detectLicense(path: String): String {

        // TODO - call model to extract license from a file

        return "PLACEHOLDER-LICENSE"
    }

    fun detect(path: String): Map<String, String> {
        val fileToLicense = mutableMapOf<String, String>()

        for (file in findLicensesFiles(path)) {
            fileToLicense[file.absolutePath] = detectLicense(file.absolutePath)
        }

        return fileToLicense
    }
}