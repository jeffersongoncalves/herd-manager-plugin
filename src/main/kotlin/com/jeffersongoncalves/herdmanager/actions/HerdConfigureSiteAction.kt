package com.jeffersongoncalves.herdmanager.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.wm.ToolWindowManager
import com.jeffersongoncalves.herdmanager.icons.HerdIcons
import com.jeffersongoncalves.herdmanager.service.HerdDetectorService

class HerdConfigureSiteAction : AnAction(
    "Configure Site",
    "Configure Laravel Herd site settings",
    HerdIcons.HERD
) {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Herd Manager")
        toolWindow?.show()
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabledAndVisible = e.project != null &&
                HerdDetectorService.getInstance().isHerdInstalled()
    }
}
