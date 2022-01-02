package com.jetbrains.sorrel.plugin.export.notification

import com.intellij.notification.Notification
import com.intellij.notification.NotificationType

class ExportLicenseDataNotification(
    titleText: String,
    type: NotificationType
) : Notification(
    "Sorrel Notifications",
    titleText, "",
    type
)
