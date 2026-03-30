package com.jeffersongoncalves.herdmanager

import com.jeffersongoncalves.herdmanager.model.HerdConfig
import org.junit.Assert.*
import org.junit.Test

class HerdConfigTest {

    @Test
    fun `fromYaml parses valid herd yml`() {
        val yaml = """
            name: my-site
            php: '8.4'
            secured: true
            aliases: {}
            services: {}
            integrations:
                forge: {}
        """.trimIndent()

        val config = HerdConfig.fromYaml(yaml)

        assertEquals("my-site", config.name)
        assertEquals("8.4", config.php)
        assertTrue(config.secured)
    }

    @Test
    fun `fromYaml handles php version without quotes`() {
        val yaml = """
            name: test-site
            php: 8.3
            secured: false
        """.trimIndent()

        val config = HerdConfig.fromYaml(yaml)

        assertEquals("test-site", config.name)
        assertEquals("8.3", config.php)
        assertFalse(config.secured)
    }

    @Test
    fun `toYaml produces correct format with single-quoted php`() {
        val config = HerdConfig(
            name = "my-app",
            php = "8.4",
            secured = true,
        )

        val yaml = config.toYaml()

        assertTrue(yaml.contains("name: my-app"))
        assertTrue(yaml.contains("php: '8.4'"))
        assertTrue(yaml.contains("secured: true"))
        assertTrue(yaml.contains("aliases: {}"))
        assertTrue(yaml.contains("services: {}"))
        assertTrue(yaml.contains("integrations:"))
        assertTrue(yaml.contains("    forge: {}"))
    }

    @Test
    fun `toYaml roundtrip preserves data`() {
        val original = HerdConfig(name = "roundtrip-test", php = "8.2", secured = false)
        val yaml = original.toYaml()
        val parsed = HerdConfig.fromYaml(yaml)

        assertEquals(original.name, parsed.name)
        assertEquals(original.php, parsed.php)
        assertEquals(original.secured, parsed.secured)
    }

    @Test
    fun `createDefault generates valid config from project name`() {
        val config = HerdConfig.createDefault("My Project Name")

        assertEquals("my-project-name", config.name)
        assertEquals("8.4", config.php)
        assertTrue(config.secured)
    }

    @Test
    fun `createDefault sanitizes special characters`() {
        val config = HerdConfig.createDefault("my_project.v2")

        assertEquals("my-project-v2", config.name)
    }

    @Test
    fun `fromYaml handles empty content gracefully`() {
        val config = HerdConfig.fromYaml("")

        assertEquals("", config.name)
        assertEquals("8.4", config.php)
        assertTrue(config.secured)
    }
}
