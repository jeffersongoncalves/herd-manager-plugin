package com.jeffersongoncalves.herdmanager.service

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project

@Service(Service.Level.APP)
class HerdCliService {

    private val log = Logger.getInstance(HerdCliService::class.java)

    fun linkSite(project: Project, siteName: String, onComplete: (() -> Unit)? = null) {
        runHerdCommand(project, listOf("link", siteName), "Linking site '$siteName'...") {
            onComplete?.invoke()
        }
    }

    fun unlinkSite(project: Project, siteName: String, onComplete: (() -> Unit)? = null) {
        runHerdCommand(project, listOf("unlink", siteName), "Unlinking site '$siteName'...") {
            onComplete?.invoke()
        }
    }

    fun secureSite(project: Project, domain: String, onComplete: (() -> Unit)? = null) {
        runHerdCommand(project, listOf("secure", domain), "Securing '$domain'...") {
            onComplete?.invoke()
        }
    }

    fun unsecureSite(project: Project, domain: String, onComplete: (() -> Unit)? = null) {
        runHerdCommand(project, listOf("unsecure", domain), "Removing SSL from '$domain'...") {
            onComplete?.invoke()
        }
    }

    fun openSite(project: Project, domain: String) {
        runHerdCommand(project, listOf("open", domain), "Opening '$domain'...")
    }

    private fun runHerdCommand(
        project: Project,
        args: List<String>,
        title: String,
        onComplete: (() -> Unit)? = null
    ) {
        val herdExe = HerdDetectorService.getInstance().getHerdExecutable()
        if (herdExe == null) {
            showNotification(project, "Herd executable not found", NotificationType.ERROR)
            return
        }

        ProgressManager.getInstance().run(object : Task.Backgroundable(project, title, false) {
            override fun run(indicator: ProgressIndicator) {
                try {
                    val commandLine = GeneralCommandLine(herdExe)
                        .withParameters(args)
                        .withWorkDirectory(project.basePath)
                        .withCharset(Charsets.UTF_8)

                    val handler = CapturingProcessHandler(commandLine)
                    val result = handler.runProcess(30_000)

                    ApplicationManager.getApplication().invokeLater {
                        if (result.exitCode == 0) {
                            val output = result.stdout.trim().ifEmpty { "Command completed successfully" }
                            showNotification(project, output, NotificationType.INFORMATION)
                        } else {
                            val error = result.stderr.trim().ifEmpty { result.stdout.trim() }
                            showNotification(project, "Error: $error", NotificationType.ERROR)
                        }
                        onComplete?.invoke()
                    }
                } catch (e: Exception) {
                    log.error("Failed to execute herd command: ${args.joinToString(" ")}", e)
                    ApplicationManager.getApplication().invokeLater {
                        showNotification(project, "Failed: ${e.message}", NotificationType.ERROR)
                        onComplete?.invoke()
                    }
                }
            }
        })
    }

    private fun showNotification(project: Project, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Herd Manager")
            .createNotification("Herd Manager", content, type)
            .notify(project)
    }

    companion object {
        fun getInstance(): HerdCliService =
            ApplicationManager.getApplication().getService(HerdCliService::class.java)
    }
}
