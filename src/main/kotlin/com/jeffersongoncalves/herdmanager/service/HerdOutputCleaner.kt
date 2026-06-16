package com.jeffersongoncalves.herdmanager.service

/**
 * Strips PHP engine noise from Herd CLI output so IDE notifications stay clean.
 *
 * Herd's bundled tooling (phpseclib inside herd.phar) emits PHP Deprecated/Warning/Notice
 * lines and phar:// stack frames alongside the real command result. Pure string logic —
 * no IntelliJ platform deps — so it is unit-testable in isolation.
 */
object HerdOutputCleaner {

    private val NOISE_PREFIX = Regex(
        """^(Deprecated|Warning|Notice|Strict Standards|Deprecation):""",
        RegexOption.IGNORE_CASE
    )

    fun clean(raw: String): String =
        raw.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { NOISE_PREFIX.containsMatchIn(it) }
            .filterNot { it.contains("phar://") }
            .joinToString("\n")
            .trim()
}
