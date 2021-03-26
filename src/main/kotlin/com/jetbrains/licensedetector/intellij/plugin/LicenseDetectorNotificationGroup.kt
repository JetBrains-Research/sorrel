package com.jetbrains.licensedetector.intellij.plugin

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup

object LicenseDetectorNotificationGroup {
    val group = NotificationGroup(
        "License Detector Notifications",
        NotificationDisplayType.BALLOON,
        true
    )
}