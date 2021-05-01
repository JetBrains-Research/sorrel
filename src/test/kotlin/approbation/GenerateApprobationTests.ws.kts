package approbation

import com.jetbrains.licensedetector.intellij.plugin.utils.ApprobationUtils.cleanProjectName
import com.jetbrains.licensedetector.intellij.plugin.utils.ApprobationUtils.processPathForInsertion
import java.io.File

/**
 * Test class text for generating
 */
val testClassTemplate =
    """
        package approbation

        import com.intellij.testFramework.HeavyPlatformTestCase
        import com.jetbrains.licensedetector.intellij.plugin.utils.ApprobationUtils
        import org.junit.Test

        class Approbation_%s : HeavyPlatformTestCase() {

            override fun setUpProject() {
                myProject = ApprobationUtils.setUpProject("%s")
            }

            @Test
            fun testApprobation() {
                ApprobationUtils.doApprobationTest(project, "%s")
            }
        }

    """.trimIndent()

//Path to directory with all test projects
val mostPopularProjectsDirPath = "C:\\Users\\pogre\\Desktop\\Java Popular Projects"
val mostPopularProjectsDir = File(mostPopularProjectsDirPath)
assert(mostPopularProjectsDir.exists())

//Path to the directory where test classes will be generated
val targetDirForGeneratedTestClasses =
    "C:\\Users\\pogre\\Desktop\\license-detector-plugin\\src\\test\\kotlin\\approbation"
//Path to the directory where the files with the testing results will be saved
val pathForResults = "C:\\Users\\pogre\\Desktop\\Approbation Result"

val projectsDirs: ArrayList<File> = arrayListOf(*mostPopularProjectsDir.listFiles { pathname ->
    pathname?.isDirectory == true && !(pathname.isHidden)
}!!)

for (projectDir in projectsDirs) {
    val filteredProjectName = cleanProjectName(projectDir.name)
    val updatedProjectPath = processPathForInsertion(projectDir.absolutePath)
    val updatedPathForResults = processPathForInsertion(pathForResults)
    val testText = String.format(testClassTemplate, filteredProjectName, updatedProjectPath, updatedPathForResults)
    println(testText)
    val testClass = File(targetDirForGeneratedTestClasses + "\\Approbation_${filteredProjectName}.kt")
    testClass.writeText(testText)
}
