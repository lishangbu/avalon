package io.github.lishangbu.avalon.ip2location

import io.github.lishangbu.avalon.ip2location.exception.EmptyIpAddressException
import io.github.lishangbu.avalon.ip2location.exception.InvalidIpAddressException
import io.github.lishangbu.avalon.ip2location.exception.IpToLocationException
import io.github.lishangbu.avalon.ip2location.exception.Ipv6NotSupportException
import io.github.lishangbu.avalon.ip2location.exception.MissingFileException
import io.github.lishangbu.avalon.ip2location.properties.IpToLocationProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class IpToLocationSupportTypesTest {
    @Test
    fun propertiesExposeDefaultLocationAndPrefix() {
        val properties = IpToLocationProperties()

        assertEquals("ip2location", IpToLocationProperties.PREFIX)
        assertTrue(properties.dbFileLocation.endsWith("IP2LOCATION-LITE-DB11.IPV6.BIN"))
    }

    @Test
    fun exceptionsExposeExpectedMessages() {
        assertEquals("IP address cannot be blank.", EmptyIpAddressException().message)
        assertEquals("Invalid IP address.", InvalidIpAddressException().message)
        assertEquals("Invalid database path.", MissingFileException().message)
        assertEquals("This BIN does not contain IPv6 data.", Ipv6NotSupportException().message)
        assertEquals("custom", IpToLocationException("custom").message)
    }
}
