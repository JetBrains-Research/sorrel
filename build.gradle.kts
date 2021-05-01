import org.jetbrains.changelog.closure
import org.jetbrains.changelog.markdownToHTML

plugins {
    // Java support
    id("java")
    // Kotlin support
    id("org.jetbrains.kotlin.jvm") version "1.4.32"
    // gradle-intellij-plugin - read more: https://github.com/JetBrains/gradle-intellij-plugin
    id("org.jetbrains.intellij") version "0.7.2"
    // gradle-changelog-plugin - read more: https://github.com/JetBrains/gradle-changelog-plugin
    id("org.jetbrains.changelog") version "0.5.0"
}

// Import variables from gradle.properties file
val pluginGroup: String by project
// `pluginName_` variable ends with `_` because of the collision with Kotlin magic getter in the `intellij` closure.
// Read more about the issue: https://github.com/JetBrains/intellij-platform-plugin-template/issues/29
val pluginName_: String by project
val pluginVersion: String by project
val pluginSinceBuild: String by project
val pluginUntilBuild: String by project

val platformType: String by project
val platformVersion: String by project
val platformDownloadSources: String by project

group = pluginGroup
version = pluginVersion

// Configure project's dependencies
repositories {
    mavenCentral()
    jcenter()
}

val priority: Configuration by configurations.creating

sourceSets.main.configure {
    compileClasspath = priority + compileClasspath
    runtimeClasspath = priority + runtimeClasspath
}
sourceSets.test.configure {
    compileClasspath = priority + compileClasspath
    runtimeClasspath = priority + runtimeClasspath
}

dependencies {
    priority(kotlin("reflect", "1.4+"))
    priority(kotlin("stdlib", "1.4+"))
    implementation("io.kinference:inference:0.1.2") {
        exclude("org.slf4j", "slf4j-log4j12")
    }
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.4.3")
}

// Configure gradle-intellij-plugin plugin.
// Read more: https://github.com/JetBrains/gradle-intellij-plugin
intellij {
    pluginName = pluginName_
    version = platformVersion
    type = platformType
    downloadSources = platformDownloadSources.toBoolean()
    updateSinceUntilBuild = true

//  Plugin Dependencies:
//  https://www.jetbrains.org/intellij/sdk/docs/basics/plugin_structure/plugin_dependencies.html
//
    setPlugins("java", "Kotlin", "maven", "gradle", "Groovy")
}

tasks {

    runIde {
        jvmArgs("--add-exports=java.base/jdk.internal.vm=ALL-UNNAMED")
        maxHeapSize = "2g"
    }

    test {
        useJUnit()
    }

    compileJava {
        sourceCompatibility = "11"
        targetCompatibility = "11"
    }

    setOf(compileKotlin, compileTestKotlin).forEach {
        it.get().kotlinOptions {
            jvmTarget = "11"
            languageVersion = "1.4"
            apiVersion = "1.4"
            // For creation of default methods in interfaces
            freeCompilerArgs = listOf("-Xjvm-default=compatibility")
        }
    }

    patchPluginXml {
        version(pluginVersion)
        sinceBuild(pluginSinceBuild)
        untilBuild(pluginUntilBuild)

        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        pluginDescription(
            closure {
                File("./README.md").readText().lines().run {
                    val start = "<!-- Plugin description -->"
                    val end = "<!-- Plugin description end -->"

                    if (!containsAll(listOf(start, end))) {
                        throw GradleException("Plugin description section not found in README.md file:\n$start ... $end")
                    }
                    subList(indexOf(start) + 1, indexOf(end))
                }.joinToString("\n").run { markdownToHTML(this) }
            }
        )

        // Get the latest available change notes from the changelog file
        changeNotes(
            closure {
                changelog.getLatest().toHTML()
            }
        )
    }

    publishPlugin {
        dependsOn("patchChangelog")
        token(System.getenv("PUBLISH_TOKEN"))
        channels(pluginVersion.split('-').getOrElse(1) { "default" }.split('.').first())
    }
}
