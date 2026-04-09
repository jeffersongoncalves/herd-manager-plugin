package com.jeffersongoncalves.herdmanager.service

import com.google.gson.JsonParser
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service(Service.Level.APP)
class HerdDetectorService {

    private val log = Logger.getInstance(HerdDetectorService::class.java)

    fun isHerdInstalled(): Boolean {
        return getHerdExecutable() != null
    }

    fun getHerdExecutable(): String? {
        val os = System.getProperty("os.name", "").lowercase()

        return when {
            os.contains("win") -> findWindowsHerd()
            os.contains("mac") || os.contains("darwin") -> findMacHerd()
            else -> findLinuxHerd()
        }
    }

    fun getHerdConfigDir(): Path? {
        val home = System.getProperty("user.home") ?: return null
        val os = System.getProperty("os.name", "").lowercase()

        val candidates = when {
            os.contains("mac") || os.contains("darwin") -> listOf(
                Paths.get(home, "Library", "Application Support", "Herd"),
                Paths.get(home, ".config", "herd")
            )
            else -> listOf(
                Paths.get(home, ".config", "herd")
            )
        }

        return candidates.firstOrNull { Files.isDirectory(it) }
    }

    fun getInstalledPhpVersions(): List<String> {
        val phpJson = findPhpConfigFile() ?: return defaultPhpVersions()

        return try {
            val content = Files.readString(phpJson)
            val json = JsonParser.parseString(content).asJsonObject
            json.keySet()
                .filter { it.startsWith("installed_") && !it.startsWith("installed_internal_") }
                .map { it.removePrefix("installed_") }
                .filter { it.matches(Regex("\\d+\\.\\d+")) }
                .sortedDescending()
        } catch (e: Exception) {
            log.warn("Failed to read PHP versions from herd config", e)
            defaultPhpVersions()
        }
    }

    fun getTld(): String {
        val valetConfig = findValetConfigFile() ?: return "test"

        return try {
            val content = Files.readString(valetConfig)
            val json = JsonParser.parseString(content).asJsonObject
            json.get("tld")?.asString ?: "test"
        } catch (e: Exception) {
            log.warn("Failed to read TLD from herd config", e)
            "test"
        }
    }

    fun getLinkedSites(): List<String> {
        val sitesDir = findValetSitesDir() ?: return emptyList()

        return try {
            Files.list(sitesDir).use { stream ->
                stream.map { it.fileName.toString() }.toList()
            }
        } catch (e: Exception) {
            log.warn("Failed to list linked sites", e)
            emptyList()
        }
    }

    fun isSiteLinked(siteName: String): Boolean {
        val sitesDir = findValetSitesDir() ?: return false
        return Files.exists(sitesDir.resolve(siteName))
    }

    private fun isMac(): Boolean {
        val os = System.getProperty("os.name", "").lowercase()
        return os.contains("mac") || os.contains("darwin")
    }

    /**
     * Finds php.json config file.
     * Windows: ~/.config/herd/config/php.json
     * macOS: ~/Library/Application Support/Herd/config/php/php.json
     *        or ~/.config/herd/config/php.json
     */
    private fun findPhpConfigFile(): Path? {
        val home = System.getProperty("user.home") ?: return null
        val candidates = mutableListOf<Path>()

        if (isMac()) {
            candidates.add(Paths.get(home, "Library", "Application Support", "Herd", "config", "php", "php.json"))
        }

        // Common path (Windows + macOS fallback)
        candidates.add(Paths.get(home, ".config", "herd", "config", "php.json"))

        return candidates.firstOrNull { Files.exists(it) }
    }

    /**
     * Finds valet config.json.
     * Windows: ~/.config/herd/config/valet/config.json
     * macOS: ~/.config/valet/config.json (standard Valet path used by Herd)
     *        or ~/.config/herd/config/valet/config.json
     */
    private fun findValetConfigFile(): Path? {
        val home = System.getProperty("user.home") ?: return null
        val candidates = mutableListOf<Path>()

        if (isMac()) {
            candidates.add(Paths.get(home, ".config", "valet", "config.json"))
        }

        // Common path (Windows + macOS fallback)
        candidates.add(Paths.get(home, ".config", "herd", "config", "valet", "config.json"))

        return candidates.firstOrNull { Files.exists(it) }
    }

    /**
     * Finds the valet Sites directory.
     * Windows: ~/.config/herd/config/valet/Sites/
     * macOS: ~/.config/valet/Sites/ (standard Valet path used by Herd)
     *        or ~/.config/herd/config/valet/Sites/
     */
    private fun findValetSitesDir(): Path? {
        val home = System.getProperty("user.home") ?: return null
        val candidates = mutableListOf<Path>()

        if (isMac()) {
            candidates.add(Paths.get(home, ".config", "valet", "Sites"))
        }

        // Common path (Windows + macOS fallback)
        candidates.add(Paths.get(home, ".config", "herd", "config", "valet", "Sites"))

        return candidates.firstOrNull { Files.isDirectory(it) }
    }

    private fun findWindowsHerd(): String? {
        val home = System.getProperty("user.home") ?: return null
        val herdBat = Paths.get(home, ".config", "herd", "bin", "herd.bat")
        if (Files.exists(herdBat)) return herdBat.toString()

        val herdExe = Paths.get(home, ".config", "herd", "bin", "herd.exe")
        if (Files.exists(herdExe)) return herdExe.toString()

        return findInPath("herd")
    }

    private fun findMacHerd(): String? {
        val home = System.getProperty("user.home") ?: return findInPath("herd")
        val candidates = listOf(
            "/opt/homebrew/bin/herd",
            "/usr/local/bin/herd",
            "$home/Library/Application Support/Herd/bin/herd",
            "$home/.config/herd/bin/herd"
        )
        return candidates.firstOrNull { Files.exists(Paths.get(it)) } ?: findInPath("herd")
    }

    private fun findLinuxHerd(): String? {
        val home = System.getProperty("user.home") ?: return null
        val herdBin = Paths.get(home, ".config", "herd", "bin", "herd")
        if (Files.exists(herdBin)) return herdBin.toString()
        return findInPath("herd")
    }

    private fun findInPath(command: String): String? {
        val pathEnv = System.getenv("PATH") ?: return null
        val separator = if (System.getProperty("os.name", "").lowercase().contains("win")) ";" else ":"
        val extensions = if (System.getProperty("os.name", "").lowercase().contains("win"))
            listOf(".bat", ".exe", ".cmd", "") else listOf("")

        for (dir in pathEnv.split(separator)) {
            for (ext in extensions) {
                val candidate = Paths.get(dir, "$command$ext")
                if (Files.exists(candidate) && Files.isExecutable(candidate)) {
                    return candidate.toString()
                }
            }
        }
        return null
    }

    private fun defaultPhpVersions(): List<String> = listOf("8.4", "8.3", "8.2", "8.1")

    companion object {
        fun getInstance(): HerdDetectorService =
            ApplicationManager.getApplication().getService(HerdDetectorService::class.java)
    }
}
