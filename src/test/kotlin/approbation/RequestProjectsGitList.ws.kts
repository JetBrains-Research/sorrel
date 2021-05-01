package approbation

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser.parseString
import com.intellij.util.io.HttpRequests
import java.io.File

/**
 * Requests git clone links for the top 100 projects on GitHub containing Java code
 */

val url = "https://api.github.com/search/repositories?q=language:java&sortr:stars&per_page=100&page=0"
val accept = "application/vnd.github.v3+json"
val timeoutInSeconds = 5

val responseText = HttpRequests.request(url)
    .accept(accept)
    .connectTimeout(timeoutInSeconds * 1000)
    .readTimeout(timeoutInSeconds * 1000)
    .readString()

val responseJson: JsonObject = parseString(responseText).asJsonObject
val items: JsonArray = responseJson["items"].asJsonArray
val stringBuilder = StringBuilder()
stringBuilder.append("git clone ")
for (item in items) {
    val cloneUrl = item.asJsonObject["clone_url"].asString
    println(cloneUrl)
    stringBuilder.append(cloneUrl)
    stringBuilder.append(" && git clone ")
}
stringBuilder.removeSuffix(" && git clone ")
val gitCloneQuery = stringBuilder.toString()
val pathToSaveGitCloneQuery = "C:\\Users\\pogre\\Desktop\\gitCloneQuery.txt"
val fileToSaveGitCloneQuery = File(pathToSaveGitCloneQuery)
fileToSaveGitCloneQuery.writeText(gitCloneQuery)