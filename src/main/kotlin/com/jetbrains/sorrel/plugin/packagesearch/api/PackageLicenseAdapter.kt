package com.jetbrains.sorrel.plugin.packagesearch.api

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.jetbrains.sorrel.plugin.detection.DetectorManager.getLicenseByNameOrSpdx
import com.jetbrains.sorrel.plugin.licenses.License
import com.jetbrains.sorrel.plugin.licenses.SupportedLicense
import com.jetbrains.sorrel.plugin.licenses.UnsupportedLicense
import java.lang.reflect.Type

object PackageLicenseAdapter : JsonDeserializer<License> {

    override fun deserialize(
            jsonElement: JsonElement,
            type: Type,
            jsonDeserializationContext: JsonDeserializationContext): License {
        val jsonObject = jsonElement.asJsonObject

        if (jsonObject.has("spdx_id")) {
            val license = getLicenseByNameOrSpdx(jsonObject["spdx_id"].asString)
            if (license is SupportedLicense) {
                return license
            }
        }

        if (jsonObject.has("name")) {
            val license = getLicenseByNameOrSpdx(jsonObject["name"].asString)
            if (license is SupportedLicense) {
                return license
            }
        }

        return jsonDeserializationContext.deserialize(jsonElement, UnsupportedLicense::class.java)
    }
}