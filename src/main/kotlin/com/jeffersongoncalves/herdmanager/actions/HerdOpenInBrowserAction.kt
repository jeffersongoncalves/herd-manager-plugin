package com.jeffersongoncalves.herdmanager.actions

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jeffersongoncalves.herdmanager.icons.HerdIcons
import com.jeffersongoncalves.herdmanager.service.HerdConfigService
import com.jeffersongoncalves.herdmanager.service.HerdDetectorService

class HerdOpenInBrowserAction : AnAction(
    "Open in Browser",
    "Open site in default browser",
    HerdIcons.HERD
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val url = HerdConfigService.getInstance(project).getSiteUrl() ?: return
        BrowserUtil.browse(url)
    }

    override fun update(e: AnActionEvent) {
        val project = e.project
        val isAvailable = project != null &&
                HerdDetectorService.getInstance().isHerdInstalled() &&
                HerdConfigService.getInstance(project).isLinked

        e.presentation.isEnabledAndVisible = isAvailable
    }
}
