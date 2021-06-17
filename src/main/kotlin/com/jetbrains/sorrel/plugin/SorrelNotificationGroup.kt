package com.jetbrains.sorrel.plugin

import com.intellij.notification.NotificationDisplayType
import com.intellij.notification.NotificationGroup

object SorrelNotificationGroup {
    val group = NotificationGroup(
        "Sorrel Notifications",
        NotificationDisplayType.BALLOON,
        true
    )
}