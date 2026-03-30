package com.jeffersongoncalves.herdmanager.ui

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.StatusBar
import com.intellij.openapi.wm.StatusBarWidget
import com.intellij.openapi.wm.StatusBarWidgetFactory
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.Consumer
import com.jeffersongoncalves.herdmanager.icons.HerdIcons
import com.jeffersongoncalves.herdmanager.service.HerdConfigService
import com.jeffersongoncalves.herdmanager.service.HerdDetectorService
import java.awt.event.MouseEvent
import javax.swing.Icon

class HerdStatusBarWidgetFactory : StatusBarWidgetFactory {

    override fun getId(): String = WIDGET_ID

    override fun getDisplayName(): String = "Herd Manager"

    override fun isAvailable(project: Project): Boolean {
        return HerdDetectorService.getInstance().isHerdInstalled()
    }

    override fun createWidget(project: Project): StatusBarWidget {
        return HerdStatusBarWidget(project)
    }

    override fun disposeWidget(widget: StatusBarWidget) {
        // nothing to dispose
    }

    companion object {
        const val WIDGET_ID = "HerdStatusBarWidget"
    }
}

class HerdStatusBarWidget(private val project: Project) : StatusBarWidget, StatusBarWidget.IconPresentation {

    private var statusBar: StatusBar? = null

    init {
        project.messageBus.connect().subscribe(
            HerdConfigService.CONFIG_CHANGED,
            object : HerdConfigService.ConfigChangeListener {
                override fun onConfigChanged() {
                    ApplicationManager.getApplication().invokeLater {
                        statusBar?.updateWidget(ID())
                    }
                }
            }
        )
    }

    override fun ID(): String = HerdStatusBarWidgetFactory.WIDGET_ID

    override fun install(statusBar: StatusBar) {
        this.statusBar = statusBar
    }

    override fun getPresentation(): StatusBarWidget.WidgetPresentation = this

    override fun getTooltipText(): String {
        val configService = HerdConfigService.getInstance(project)
        return if (configService.isLinked) {
            configService.getSiteUrl() ?: "Herd: Linked"
        } else {
            "Herd: Click to configure"
        }
    }

    override fun getIcon(): Icon {
        val configService = HerdConfigService.getInstance(project)
        return if (configService.isLinked) HerdIcons.LINKED else HerdIcons.UNLINKED
    }

    override fun getClickConsumer(): Consumer<MouseEvent> {
        return Consumer {
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("Herd Manager")
            toolWindow?.show()
        }
    }

    override fun dispose() {
        statusBar = null
    }
}
