import java.io.File

class Vectorizer constructor(val vocabulary: List<String>){
    val vector_dim = vocabulary.size

    fun vectorize(text: String): IntArray {
        // Initialize empty vector for text
        val text_vector = IntArray(vector_dim)

        // Iterate over each feature
        for (index in 0 until vector_dim) {
            // Get feature text & initialize occurance
            val feature = vocabulary.get(index)
            var feature_occurances = 0

            // Count how many times feature occurs in text
            for (window in text.windowed(feature.length)) {
                if (window.equals(feature)) {
                    feature_occurances += 1
                }
            }

            // Update vector component with counted value
            text_vector[index] = feature_occurances
        }

        return text_vector
    }

    fun vectorize(file: File): IntArray {
        return vectorize(file.readText())
    }

    fun getFeatureIndex(feature: String): Int {
        var index = -1

        for (i in 1..vector_dim) {
            if (vocabulary[i].equals(feature)) {
                index = i
                break
            }
        }

        return index
    }

    fun getFeature(index: Int): String {
        return vocabulary[index]
    }
}