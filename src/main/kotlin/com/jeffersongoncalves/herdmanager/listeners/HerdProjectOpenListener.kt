package com.jeffersongoncalves.herdmanager.listeners

import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ToolWindowManager
import com.jeffersongoncalves.herdmanager.service.HerdConfigService
import com.jeffersongoncalves.herdmanager.service.HerdDetectorService

class HerdProjectOpenListener : ProjectActivity {

    override suspend fun execute(project: Project) {
        val detector = HerdDetectorService.getInstance()
        if (!detector.isHerdInstalled()) return

        val configService = HerdConfigService.getInstance(project)

        if (!configService.hasConfig()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Herd Manager")
                .createNotification(
                    "Laravel Herd",
                    "No herd.yml found. Would you like to configure this project for Herd?",
                    NotificationType.INFORMATION
                )
                .addAction(NotificationAction.createSimpleExpiring("Configure Now") {
                    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Herd Manager")
                    toolWindow?.show()
                })
                .notify(project)
        } else if (!configService.isLinked) {
            val siteName = configService.config?.name ?: return
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Herd Manager")
                .createNotification(
                    "Laravel Herd",
                    "Site '$siteName' is configured but not linked. Would you like to link it?",
                    NotificationType.INFORMATION
                )
                .addAction(NotificationAction.createSimpleExpiring("Link Now") {
                    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Herd Manager")
                    toolWindow?.show()
                })
                .notify(project)
        }
    }
}
