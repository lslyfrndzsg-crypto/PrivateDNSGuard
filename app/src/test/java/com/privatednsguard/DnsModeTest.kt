package com.privatednsguard

import com.privatednsguard.data.model.DnsMode
import org.junit.Assert.assertEquals
import org.junit.Test

class DnsModeTest {

    @Test
    fun fromSystemValue_parsesCorrectly() {
        assertEquals(DnsMode.HOSTNAME, DnsMode.fromSystemValue("hostname"))
        assertEquals(DnsMode.HOSTNAME, DnsMode.fromSystemValue("HOSTNAME"))
        assertEquals(DnsMode.OPPORTUNISTIC, DnsMode.fromSystemValue("opportunistic"))
        assertEquals(DnsMode.OFF, DnsMode.fromSystemValue("off"))
        assertEquals(DnsMode.OPPORTUNISTIC, DnsMode.fromSystemValue(null))
        assertEquals(DnsMode.OPPORTUNISTIC, DnsMode.fromSystemValue("unknown_value"))
    }
}
