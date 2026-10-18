package com.privatednsguard

import com.privatednsguard.data.model.DnsValidation
import com.privatednsguard.data.model.ValidationResult
import org.junit.Assert.assertTrue
import org.junit.Test

class DnsValidationTest {

    @Test
    fun validHostnames_returnValid() {
        val validList = listOf(
            "dns.nextdns.io",
            "2b6c91.dns.nextdns.io",
            "dns.quad9.net",
            "one.one.one.one",
            "security.cloudflare-dns.com",
            "dns.adguard-dns.com",
            "dns.mullvad.net",
            "my-custom-dns.example.org"
        )

        for (host in validList) {
            val result = DnsValidation.validateDoTHostname(host)
            assertTrue("Expected $host to be valid, but was $result", result is ValidationResult.Valid)
        }
    }

    @Test
    fun invalidHostnames_returnInvalid() {
        val invalidList = listOf(
            "",
            "   ",
            null,
            "http://dns.nextdns.io",
            "https://dns.nextdns.io",
            "dns.nextdns.io:853",
            "dns.nextdns.io/path",
            "nodotsinthisname",
            "invalid_characters*.com"
        )

        for (host in invalidList) {
            val result = DnsValidation.validateDoTHostname(host)
            assertTrue("Expected '$host' to be invalid, but was $result", result is ValidationResult.Invalid)
        }
    }
}
