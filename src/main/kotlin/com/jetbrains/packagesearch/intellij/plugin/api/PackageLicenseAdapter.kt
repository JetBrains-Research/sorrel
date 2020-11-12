package com.jetbrains.packagesearch.intellij.plugin.api

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.jetbrains.licensedetector.intellij.plugin.licenses.License
import com.jetbrains.licensedetector.intellij.plugin.licenses.UnsupportedLicense
import com.jetbrains.licensedetector.intellij.plugin.licenses.getLicenseOnNameOrNull
import com.jetbrains.licensedetector.intellij.plugin.licenses.getLicenseOnSpdxIdOrNull
import java.lang.reflect.Type

object PackageLicenseAdapter : JsonDeserializer<License> {

    override fun deserialize(
            jsonElement: JsonElement,
            type: Type,
            jsonDeserializationContext: JsonDeserializationContext): License {
        val jsonObject = jsonElement.asJsonObject

        if (jsonObject.has("spdx_id")) {
            val license = getLicenseOnSpdxIdOrNull(jsonObject["spdx_id"].asString)
            if (license != null) {
                return license
            }
        }

        if (jsonObject.has("name")) {
            val license = getLicenseOnNameOrNull(jsonObject["name"].asString)
            if (license != null) {
                return license
            }
        }

        return jsonDeserializationContext.deserialize(jsonElement, UnsupportedLicense::class.java)
    }
}