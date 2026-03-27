package io.github.lishangbu.avalon.ip2location.autoconfigure

import com.ip2location.IP2Location
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.ApplicationContextRunner

class Ip2LocationAutoConfigurationTest {
    private val contextRunner =
        ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(Ip2LocationAutoConfiguration::class.java))

    @Test
    fun registersOfficialIp2LocationBean() {
        contextRunner.run { context ->
            assertThat(context).hasSingleBean(IP2Location::class.java)

            val ip2Location = context.getBean(IP2Location::class.java)
            val result = ip2Location.ipQuery("39.189.23.43")

            assertThat(result.status).isEqualTo("OK")
            assertThat(result.countryShort).isEqualTo("CN")
            assertThat(result.countryLong).isEqualTo("China")
            assertThat(result.region).isEqualTo("Zhejiang")
            assertThat(result.city).isEqualTo("Ningbo")
            assertThat(result.timeZone).isEqualTo("+08:00")
        }
    }

    @Test
    fun backsOffWhenUserProvidesOfficialBean() {
        contextRunner
            .withBean(IP2Location::class.java, { IP2Location() })
            .run { context ->
                assertThat(context).hasSingleBean(IP2Location::class.java)
            }
    }

    @Test
    fun failsFastWhenDatabaseResourceIsMissing() {
        contextRunner
            .withPropertyValues("ip2location.db-file-location=classpath:missing-ip2location.bin")
            .run { context ->
                assertThat(context).hasFailed()
                assertThat(context.startupFailure).hasMessageContaining("missing-ip2location.bin")
            }
    }

    @Test
    fun exposesOfficialStatusForEmptyIpAddress() {
        contextRunner.run { context ->
            val ip2Location = context.getBean(IP2Location::class.java)

            assertThat(ip2Location.ipQuery("").status).isEqualTo("EMPTY_IP_ADDRESS")
        }
    }
}
