package com.jeffersongoncalves.herdmanager

import com.jeffersongoncalves.herdmanager.service.HerdOutputCleaner
import org.junit.Assert.*
import org.junit.Test

class HerdOutputCleanerTest {

    @Test
    fun `strips php deprecation noise but keeps real output`() {
        val raw = """
            Deprecated: chr(): Providing a value not in-between 0 and 255 is deprecated in phar://C:/Users/simao/.config/herd/bin/herd.phar/vendor/phpseclib/phpseclib/phpseclib/Crypt/RSA/PrivateKey.php on line 219
            Deprecated: chr(): Providing a value not in-between 0 and 255 is deprecated in phar://C:/Users/simao/.config/herd/bin/herd.phar/vendor/phpseclib/phpseclib/phpseclib/Crypt/RSA/PrivateKey.php on line 219
            Restarting NGINX...
            The [sami-erp.test] site has been secured with a fresh TLS certificate.
        """.trimIndent()

        val cleaned = HerdOutputCleaner.clean(raw)

        assertEquals(
            "Restarting NGINX...\nThe [sami-erp.test] site has been secured with a fresh TLS certificate.",
            cleaned
        )
    }

    @Test
    fun `filters warning notice and strict standards prefixes`() {
        val raw = """
            Warning: something off
            Notice: heads up
            Strict Standards: be careful
            Site linked.
        """.trimIndent()

        assertEquals("Site linked.", HerdOutputCleaner.clean(raw))
    }

    @Test
    fun `drops standalone phar stack frames`() {
        val raw = """
            #0 phar://C:/herd.phar/foo.php(10): bar()
            in phar://C:/herd.phar/foo.php on line 10
            Done.
        """.trimIndent()

        assertEquals("Done.", HerdOutputCleaner.clean(raw))
    }

    @Test
    fun `returns empty when only noise present`() {
        val raw = "Deprecated: x in phar://y on line 1"
        assertEquals("", HerdOutputCleaner.clean(raw))
    }

    @Test
    fun `passes clean output through untouched`() {
        val raw = "The [app.test] site has been linked."
        assertEquals(raw, HerdOutputCleaner.clean(raw))
    }

    @Test
    fun `trims blank lines and surrounding whitespace`() {
        val raw = "\n\n  Linked.  \n\n"
        assertEquals("Linked.", HerdOutputCleaner.clean(raw))
    }
}
