package approbation

import com.intellij.testFramework.HeavyPlatformTestCase
import com.jetbrains.sorrel.plugin.utils.ApprobationUtils
import org.junit.Ignore
import org.junit.Test

/**
 * Reference approbation class for generating classes for each test project.
 * Since I could not find a way to test a variety of projects with a single test class.
 */
@Ignore
class ApprobationTestSingleProject : HeavyPlatformTestCase() {

    override fun setUpProject() {
        myProject = ApprobationUtils.setUpProject("")
    }

    @Test
    fun testApprobation() {
        ApprobationUtils.doApprobationTest(project, "")
    }
}
