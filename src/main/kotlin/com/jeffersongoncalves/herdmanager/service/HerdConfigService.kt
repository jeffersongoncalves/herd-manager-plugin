package com.jeffersongoncalves.herdmanager.service

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.util.messages.Topic
import com.jeffersongoncalves.herdmanager.model.HerdConfig
import java.io.File

@Service(Service.Level.PROJECT)
class HerdConfigService(private val project: Project) {

    private val log = Logger.getInstance(HerdConfigService::class.java)

    var config: HerdConfig? = null
        private set

    var isLinked: Boolean = false
        private set

    init {
        project.messageBus.connect().subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                val projectBase = project.basePath ?: return
                for (event in events) {
                    val path = event.path ?: continue
                    if (!path.replace("\\", "/").startsWith(projectBase.replace("\\", "/"))) continue
                    val fileName = path.substringAfterLast("/").substringAfterLast("\\")
                    if (fileName != "herd.yml") continue

                    when (event) {
                        is VFileCreateEvent, is VFileContentChangeEvent -> loadConfig()
                        is VFileDeleteEvent -> {
                            config = null
                            isLinked = false
                            fireConfigChanged()
                        }
                    }
                }
            }
        })

        loadConfig()
    }

    fun loadConfig() {
        val herdYml = findHerdYml()
        if (herdYml != null) {
            try {
                val content = String(herdYml.contentsToByteArray(), Charsets.UTF_8)
                config = HerdConfig.fromYaml(content)
            } catch (e: Exception) {
                log.warn("Failed to parse herd.yml", e)
                config = null
            }
        } else {
            config = null
        }

        checkLinkStatus()
        fireConfigChanged()
    }

    fun saveConfig(newConfig: HerdConfig) {
        WriteAction.run<Exception> {
            val baseDir = ProjectRootManager.getInstance(project).contentRoots.firstOrNull() ?: return@run
            val file = baseDir.findChild("herd.yml") ?: baseDir.createChildData(this, "herd.yml")
            VfsUtil.saveText(file, newConfig.toYaml())
        }
        config = newConfig
        checkLinkStatus()
        updateEnvAppUrl(newConfig)
        fireConfigChanged()
    }

    private fun updateEnvAppUrl(cfg: HerdConfig) {
        if (cfg.name.isBlank()) return
        val basePath = project.basePath ?: return
        val envFile = File(basePath, ".env")
        if (!envFile.exists()) return

        val tld = HerdDetectorService.getInstance().getTld()
        val protocol = if (cfg.secured) "https" else "http"
        val newUrl = "$protocol://${cfg.name}.$tld"

        val content = envFile.readText(Charsets.UTF_8)
        val lineRegex = Regex("(?m)^APP_URL=.*$")
        val currentMatch = lineRegex.find(content)
        if (currentMatch != null && currentMatch.value == "APP_URL=$newUrl") return

        val newContent = if (currentMatch != null) {
            lineRegex.replace(content, "APP_URL=$newUrl")
        } else {
            content.trimEnd('\n') + "\nAPP_URL=$newUrl\n"
        }

        envFile.writeText(newContent, Charsets.UTF_8)
        LocalFileSystem.getInstance().refreshAndFindFileByIoFile(envFile)
    }

    fun hasConfig(): Boolean = config != null

    fun getSiteUrl(): String? {
        val cfg = config ?: return null
        if (cfg.name.isBlank()) return null
        val tld = HerdDetectorService.getInstance().getTld()
        val protocol = if (cfg.secured) "https" else "http"
        return "$protocol://${cfg.name}.$tld"
    }

    fun checkLinkStatus() {
        val cfg = config
        isLinked = if (cfg != null && cfg.name.isNotBlank()) {
            HerdDetectorService.getInstance().isSiteLinked(cfg.name)
        } else {
            false
        }
    }

    fun getDefaultConfig(): HerdConfig = HerdConfig.createDefault(getFolderName())

    fun getExpectedSiteName(): String = HerdConfig.createDefault(getFolderName()).name

    fun getFolderName(): String =
        ProjectRootManager.getInstance(project).contentRoots.firstOrNull()?.name ?: project.name

    private fun findHerdYml(): VirtualFile? {
        val baseDir = ProjectRootManager.getInstance(project).contentRoots.firstOrNull() ?: return null
        return baseDir.findChild("herd.yml")
    }

    private fun fireConfigChanged() {
        project.messageBus.syncPublisher(CONFIG_CHANGED).onConfigChanged()
    }

    interface ConfigChangeListener {
        fun onConfigChanged()
    }

    companion object {
        @JvmField
        val CONFIG_CHANGED: Topic<ConfigChangeListener> = Topic.create(
            "HerdConfigChanged",
            ConfigChangeListener::class.java
        )

        fun getInstance(project: Project): HerdConfigService =
            project.getService(HerdConfigService::class.java)
    }
}
