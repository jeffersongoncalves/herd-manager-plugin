package com.jeffersongoncalves.herdmanager.ui

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.JBUI
import com.jeffersongoncalves.herdmanager.icons.HerdIcons
import com.jeffersongoncalves.herdmanager.service.HerdCliService
import com.jeffersongoncalves.herdmanager.service.HerdConfigService
import com.jeffersongoncalves.herdmanager.service.HerdDetectorService
import javax.swing.*

class HerdToolWindowPanel(private val project: Project) : JPanel() {

    private val configService = HerdConfigService.getInstance(project)
    private val detectorService = HerdDetectorService.getInstance()
    private val cliService = HerdCliService.getInstance()

    private val siteNameField = JTextField(20)
    private val phpVersionCombo = JComboBox<String>()
    private val securedCheckbox = JCheckBox("Enable HTTPS")
    private val statusLabel = JBLabel()
    private val urlLabel = JBLabel()

    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = JBUI.Borders.empty(8)

        populatePhpVersions()
        loadCurrentConfig()
        buildUI()
        subscribeToChanges()
    }

    private fun buildUI() {
        removeAll()

        val mainPanel = panel {
            group("Status") {
                row {
                    cell(statusLabel)
                }
                row("URL:") {
                    cell(urlLabel)
                    button("Open in Browser") {
                        val url = configService.getSiteUrl()
                        if (url != null) {
                            BrowserUtil.browse(url)
                        }
                    }.enabled(configService.isLinked)
                }
            }

            group("Site Configuration") {
                row("Site Name:") {
                    cell(siteNameField).align(AlignX.FILL)
                    button("Use Folder Name (${configService.getFolderName()})") {
                        siteNameField.text = configService.getExpectedSiteName()
                        buildUI()
                    }.visible(
                        configService.hasConfig() &&
                            configService.config?.name != configService.getExpectedSiteName()
                    )
                }
                row("PHP Version:") {
                    cell(phpVersionCombo)
                }
                row {
                    cell(securedCheckbox)
                }
            }

            group("Actions") {
                row {
                    button("Save Configuration") { saveConfig() }
                    // ponytail: inside a parked folder Herd already serves the site; `herd link` would only add a conflicting symlink
                    button("Link Site") { linkSite() }.visible(!configService.isInParkedFolder)
                    button("Secure Site") { secureSite() }
                        .visible(configService.isParked && configService.needsSecure())
                }
                row {
                    button("Unlink Site") { unlinkSite() }
                        .enabled(configService.isLinked)
                        .visible(!configService.isInParkedFolder)
                }
            }
        }

        add(mainPanel)
        updateStatus()
        revalidate()
        repaint()
    }

    private fun populatePhpVersions() {
        phpVersionCombo.removeAllItems()
        val versions = detectorService.getInstalledPhpVersions()
        for (version in versions) {
            phpVersionCombo.addItem(version)
        }
    }

    private fun loadCurrentConfig() {
        val config = configService.config
        if (config != null) {
            siteNameField.text = config.name
            phpVersionCombo.selectedItem = config.php
            securedCheckbox.isSelected = config.secured
        } else {
            val defaultConfig = configService.getDefaultConfig()
            siteNameField.text = defaultConfig.name
            phpVersionCombo.selectedItem = defaultConfig.php
            securedCheckbox.isSelected = defaultConfig.secured
        }
    }

    private fun updateStatus() {
        if (configService.isLinked) {
            statusLabel.icon = HerdIcons.LINKED
            statusLabel.text = if (configService.isParked) "Parked (served by Herd)" else "Linked"
            statusLabel.foreground = JBColor(JBColor.GREEN.darker(), JBColor.GREEN)
        } else if (configService.hasConfig()) {
            statusLabel.icon = HerdIcons.UNLINKED
            statusLabel.text = if (configService.isInParkedFolder) {
                "In Herd folder — set Site Name to the folder name and save"
            } else {
                "Not Linked"
            }
            statusLabel.foreground = JBColor(JBColor.ORANGE.darker(), JBColor.ORANGE)
        } else {
            statusLabel.icon = HerdIcons.UNLINKED
            statusLabel.text = "Not Configured"
            statusLabel.foreground = JBColor.GRAY
        }

        val url = configService.getSiteUrl()
        urlLabel.text = url ?: "N/A"
    }

    private fun saveConfig() {
        val config = com.jeffersongoncalves.herdmanager.model.HerdConfig(
            name = siteNameField.text.trim(),
            php = phpVersionCombo.selectedItem?.toString() ?: "8.4",
            secured = securedCheckbox.isSelected,
        )
        configService.saveConfig(config)
    }

    private fun linkSite() {
        val siteName = siteNameField.text.trim()
        if (siteName.isBlank()) return

        if (!configService.hasConfig()) {
            saveConfig()
        }

        cliService.linkSite(project, siteName) {
            ApplicationManager.getApplication().invokeLater {
                val config = configService.config
                if (config != null && config.secured) {
                    val tld = detectorService.getTld()
                    cliService.secureSite(project, "$siteName.$tld") {
                        ApplicationManager.getApplication().invokeLater {
                            configService.loadConfig()
                            refreshUI()
                        }
                    }
                } else {
                    configService.loadConfig()
                    refreshUI()
                }
            }
        }
    }

    private fun secureSite() {
        val siteName = configService.config?.name ?: return
        cliService.secureSite(project, "$siteName.${detectorService.getTld()}") {
            ApplicationManager.getApplication().invokeLater {
                configService.loadConfig()
                refreshUI()
            }
        }
    }

    private fun unlinkSite() {
        val siteName = siteNameField.text.trim()
        if (siteName.isBlank()) return

        cliService.unlinkSite(project, siteName) {
            ApplicationManager.getApplication().invokeLater {
                configService.loadConfig()
                refreshUI()
            }
        }
    }

    private fun refreshUI() {
        loadCurrentConfig()
        updateStatus()
        buildUI()
    }

    private fun subscribeToChanges() {
        project.messageBus.connect().subscribe(
            HerdConfigService.CONFIG_CHANGED,
            object : HerdConfigService.ConfigChangeListener {
                override fun onConfigChanged() {
                    ApplicationManager.getApplication().invokeLater {
                        loadCurrentConfig()
                        updateStatus()
                    }
                }
            }
        )
    }
}
