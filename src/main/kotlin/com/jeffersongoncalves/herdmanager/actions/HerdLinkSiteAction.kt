package com.jeffersongoncalves.herdmanager.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jeffersongoncalves.herdmanager.icons.HerdIcons
import com.jeffersongoncalves.herdmanager.service.HerdCliService
import com.jeffersongoncalves.herdmanager.service.HerdConfigService
import com.jeffersongoncalves.herdmanager.service.HerdDetectorService

class HerdLinkSiteAction : AnAction(
    "Link Site",
    "Link this project to Laravel Herd",
    HerdIcons.HERD
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val configService = HerdConfigService.getInstance(project)

        if (!configService.hasConfig()) {
            val defaultConfig = configService.getDefaultConfig()
            configService.saveConfig(defaultConfig)
        }

        val config = configService.config ?: return
        val cliService = HerdCliService.getInstance()

        cliService.linkSite(project, config.name) {
            if (config.secured) {
                val tld = HerdDetectorService.getInstance().getTld()
                cliService.secureSite(project, "${config.name}.$tld") {
                    configService.loadConfig()
                }
            } else {
                configService.loadConfig()
            }
        }
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        e.presentation.isEnabledAndVisible = project != null &&
                HerdDetectorService.getInstance().isHerdInstalled()
    }
}
