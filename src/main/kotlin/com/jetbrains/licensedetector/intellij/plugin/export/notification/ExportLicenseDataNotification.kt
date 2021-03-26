package com.jetbrains.licensedetector.intellij.plugin.export.notification

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.jetbrains.licensedetector.intellij.plugin.LicenseDetectorNotificationGroup

class ExportLicenseDataNotification(
    titleText: String,
    type: NotificationType
) : Notification(
    LicenseDetectorNotificationGroup.group.displayId,
    titleText, "",
    type, null
)
