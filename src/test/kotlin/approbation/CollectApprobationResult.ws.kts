package approbation

import java.io.File

// Path to approbation result directory
val pathToApprobationResult = "C:\\Users\\pogre\\Desktop\\Approbation Result"
val approbationResultDir: File = File(pathToApprobationResult)
val resultFiles: Array<File> = approbationResultDir.listFiles { pathname ->
    pathname?.name?.startsWith("library_") ?: false
}!!

var countAllIssues = 0
val countIssuesByLicense = mutableMapOf<Pair<String, String>, Int>()
val licenseUniqueLibraries = mutableMapOf<Pair<String, String>, Int>()
val countIssuesPerLibrary = mutableMapOf<String, Int>()

for (file in resultFiles) {
    val lines = file.readLines()
    countAllIssues += lines.size

    lines.forEach {
        val moduleLicense = it.substringBefore(" --- ").substringAfter(" - ")
        val libraryLicense = it.substringAfter(" --- ").substringAfter(" - ")
        val libraryName = it.substringAfter(" --- ").substringBefore(" - ")

        val newInfoForModAndLib = Pair(moduleLicense, libraryLicense)

        countIssuesByLicense[newInfoForModAndLib] = countIssuesByLicense.getOrDefault(newInfoForModAndLib, 0) + 1

        val newInfoForLib = Pair(libraryName, libraryLicense)

        licenseUniqueLibraries[newInfoForLib] =
            licenseUniqueLibraries.getOrDefault(newInfoForLib, 0) + 1

        countIssuesPerLibrary[libraryName] = countIssuesPerLibrary.getOrDefault(libraryName, 0) + 1
    }
}

//Path to processed approbation result
val pathToProcessedApprobationResult = "C:\\Users\\pogre\\Desktop\\Approbation Final"
val pathToIssuesByLicenses = "$pathToProcessedApprobationResult\\issues by licenses.txt"
val pathToIssuesByLicensesWithUniqueLibrary =
    "$pathToProcessedApprobationResult\\issues by licenses with unique library.txt"


println("Total: $countAllIssues")

println("Number of unique libraries: " + countIssuesPerLibrary.size)

val countIssuesByLicenseSB = StringBuilder("")
for (entry in countIssuesByLicense.entries.toList().sortedByDescending { it.value }) {
    countIssuesByLicenseSB.append("${entry.key.first} -> ${entry.key.second} --- ${entry.value}")
    countIssuesByLicenseSB.append("\n")
}
File(pathToIssuesByLicenses).writeText(countIssuesByLicenseSB.toString())

val licenseUniqueLibrarySB = StringBuilder("")
licenseUniqueLibraries.keys.groupingBy { it.second }.eachCount().toList().sortedByDescending { it.second }
    .forEach { entry ->
        licenseUniqueLibrarySB.append(
            "${entry.first} --- ${entry.second}"
        )
        licenseUniqueLibrarySB.append("\n")
    }

File(pathToIssuesByLicensesWithUniqueLibrary).writeText(
    licenseUniqueLibrarySB.toString()
)
