package com.jeffersongoncalves.herdmanager.model

import org.yaml.snakeyaml.LoaderOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor

data class HerdConfig(
    var name: String = "",
    var php: String = "8.4",
    var secured: Boolean = true,
) {
    fun toYaml(): String = buildString {
        appendLine("name: $name")
        appendLine("php: '$php'")
        appendLine("secured: $secured")
        appendLine("aliases: {}")
        appendLine("services: {}")
        appendLine("integrations:")
        append("    forge: {}")
    }

    companion object {
        fun fromYaml(content: String): HerdConfig {
            val yaml = Yaml(SafeConstructor(LoaderOptions()))
            val data = yaml.load<Map<String, Any>>(content) ?: return HerdConfig()

            return HerdConfig(
                name = data["name"]?.toString() ?: "",
                php = data["php"]?.toString() ?: "8.4",
                secured = data["secured"] as? Boolean ?: true,
            )
        }

        fun createDefault(projectName: String): HerdConfig {
            return HerdConfig(
                name = projectName.lowercase().replace(Regex("[^a-z0-9-]"), "-"),
                php = "8.4",
                secured = true,
            )
        }
    }
}
