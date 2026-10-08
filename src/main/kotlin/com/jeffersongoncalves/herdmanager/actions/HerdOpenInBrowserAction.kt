package com.jeffersongoncalves.herdmanager.actions

import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jeffersongoncalves.herdmanager.service.HerdConfigService
import com.jeffersongoncalves.herdmanager.service.HerdDetectorService

class HerdOpenInBrowserAction : AnAction(
    "Open in Browser",
    "Open site in default browser",
    AllIcons.General.Web
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val url = HerdConfigService.getInstance(project).getSiteUrl() ?: return
        BrowserUtil.browse(url)
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun update(e: AnActionEvent) {
        val project = e.project
        val herdInstalled = project != null && HerdDetectorService.getInstance().isHerdInstalled()

        // Visible whenever Herd is installed; enabled only when herd.yml exists and the site is linked
        e.presentation.isVisible = herdInstalled
        e.presentation.isEnabled = herdInstalled && HerdConfigService.getInstance(project!!).isLinked
    }
}
