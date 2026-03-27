package io.github.lishangbu.avalon.ip2location

import io.github.lishangbu.avalon.ip2location.properties.IpToLocationProperties
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.core.io.ClassPathResource

class IpToLocationSupportTypesTest {
    @Test
    fun propertiesExposeDefaultLocationAndPrefix() {
        val properties = IpToLocationProperties()

        assertThat(IpToLocationProperties.PREFIX).isEqualTo("ip2location")
        assertThat(properties.dbFileLocation).endsWith("IP2LOCATION-LITE-DB11.IPV6.BIN")
    }

    @Test
    fun autoConfigurationImportsOfficialStarterEntry() {
        val imports =
            ClassPathResource("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")
                .inputStream
                .bufferedReader()
                .use { it.readText() }

        assertThat(imports.trim()).isEqualTo(
            "io.github.lishangbu.avalon.ip2location.autoconfigure.Ip2LocationAutoConfiguration",
        )
    }
}
