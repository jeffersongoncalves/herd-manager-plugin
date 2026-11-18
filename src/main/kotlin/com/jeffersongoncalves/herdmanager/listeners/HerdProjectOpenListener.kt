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
        } else if (!configService.isLinked && configService.isInParkedFolder) {
            val siteName = configService.config?.name ?: return
            val folderSite = configService.getExpectedSiteName()
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Herd Manager")
                .createNotification(
                    "Laravel Herd",
                    "This project is inside a Herd folder and is already served as '$folderSite', " +
                        "not '$siteName'. Use the folder name as site name?",
                    NotificationType.INFORMATION
                )
                .addAction(NotificationAction.createSimpleExpiring("Use '$folderSite'") {
                    configService.useFolderName()
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
        } else if (configService.isParked && configService.needsSecure()) {
            val siteName = configService.config?.name ?: return
            NotificationGroupManager.getInstance()
                .getNotificationGroup("Herd Manager")
                .createNotification(
                    "Laravel Herd",
                    "Site '$siteName' is served by Herd (parked folder) but HTTPS is not set up yet.",
                    NotificationType.INFORMATION
                )
                .addAction(NotificationAction.createSimpleExpiring("Secure Now") {
                    val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Herd Manager")
                    toolWindow?.show()
                })
                .notify(project)
        }
    }
}
