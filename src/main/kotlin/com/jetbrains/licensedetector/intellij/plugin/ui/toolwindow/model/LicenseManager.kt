package com.jetbrains.licensedetector.intellij.plugin.ui.toolwindow.model

import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorBundle
import com.jetbrains.licensedetector.intellij.plugin.licenses.ALL_SUPPORTED_LICENSE
import com.jetbrains.licensedetector.intellij.plugin.licenses.NoLicense
import com.jetbrains.licensedetector.intellij.plugin.licenses.SupportedLicense
import com.jetbrains.licensedetector.intellij.plugin.licenses.getCompatiblePackageLicenses
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.Property

class LicenseManager(
        lifetime: Lifetime,
        private val installedPackages: Property<Map<String, LicenseDetectorDependency>>
) {
    val mainProjectLicense = Property<SupportedLicense>(NoLicense)
    val mainProjectCompatibleLicenses = Property<List<SupportedLicense>>(listOf())
    private val projectLicensesCompatibleWithPackageLicenses = Property<Set<SupportedLicense>>(setOf())

    //TODO: Update when ml model is ready
    private val projectLicenseCompatibleWithSourceLicenses = Property(ALL_SUPPORTED_LICENSE.toSet())

    val compatibilityIssues = Property<List<String>>(listOf())

    init {
        updatingProjectMainCompatibleLicense(lifetime)
    }

    private fun updatingProjectMainCompatibleLicense(lifetime: Lifetime) {
        projectLicensesCompatibleWithPackageLicenses.advise(lifetime) { setLicenses ->
            mainProjectCompatibleLicenses.value = setLicenses
                    .intersect(projectLicenseCompatibleWithSourceLicenses.value)
                    .toList().sortedByDescending { it.priority }
        }

        projectLicenseCompatibleWithSourceLicenses.advise(lifetime) { setLicenses ->
            mainProjectCompatibleLicenses.value = setLicenses
                    .intersect(projectLicensesCompatibleWithPackageLicenses.value)
                    .toList().sortedByDescending { it.priority }
        }

        mainProjectLicense.advise(lifetime) {
            checkCompatibilityWithProjectLicense(it, installedPackages.value.values)
        }
    }

    fun updateProjectLicensesCompatibilityWithPackagesLicenses(
            packages: Collection<LicenseDetectorDependency>
    ) {
        val licensesAllPackages = packages.fold(mutableSetOf<SupportedLicense>()) { acc, dependency ->
            //Add main package license to set
            val mainPackageLicense = dependency.remoteInfo?.licenses?.mainLicense
            if (mainPackageLicense is SupportedLicense) {
                acc.add(mainPackageLicense)
            }

            //Add other package licenses to set
            dependency.remoteInfo?.licenses?.otherLicenses?.forEach {
                if (it is SupportedLicense) {
                    acc.add(it)
                }
            }

            acc
        }

        val compatibleProjectLicenses = getCompatiblePackageLicenses(licensesAllPackages)
        projectLicensesCompatibleWithPackageLicenses.set(compatibleProjectLicenses)


        if (!compatibleProjectLicenses.contains(mainProjectLicense.value)) {
            checkCompatibilityWithProjectLicense(mainProjectLicense.value, packages)
        }
    }

    private fun checkCompatibilityWithProjectLicense(
            mainProjectLicense: SupportedLicense,
            packages: Collection<LicenseDetectorDependency>
    ) {
        var hasCompatibleIssue = false
        val stringBuilder = StringBuilder(
                LicenseDetectorBundle.message(
                        "licensedetector.ui.compatibilityIssues.projectAndDependency.head"
                )
        )
        stringBuilder.append("<ul>")

        // Add compatibility issues if needed
        packages.forEach { dependency ->

            val mainPackageLicense = dependency.remoteInfo?.licenses?.mainLicense
            if (mainPackageLicense is SupportedLicense &&
                    !mainPackageLicense.compatibleDependencyLicenses.contains(mainProjectLicense)) {
                hasCompatibleIssue = true
                stringBuilder.append("<li>" +
                        LicenseDetectorBundle.message(
                                "licensedetector.ui.compatibilityIssues.projectAndDependency",
                                dependency.identifier,
                                mainPackageLicense.name,
                                mainProjectLicense.name
                        ) + "</li>"
                )
            }
            dependency.remoteInfo?.licenses?.otherLicenses?.forEach {
                if (it is SupportedLicense &&
                        !it.compatibleDependencyLicenses.contains(mainProjectLicense)) {
                    hasCompatibleIssue = true
                    stringBuilder.append("<li>" +
                            LicenseDetectorBundle.message(
                                    "licensedetector.ui.compatibilityIssues.projectAndDependency",
                                    dependency.identifier,
                                    it.name,
                                    mainProjectLicense.name
                            ) + "</li>"
                    )
                }
            }
        }

        stringBuilder.append("</ul>")

        return if (hasCompatibleIssue) {
            compatibilityIssues.set(listOf(stringBuilder.toString()))
        } else {
            compatibilityIssues.set(listOf())
        }
    }
}