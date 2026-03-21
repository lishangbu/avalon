package io.github.lishangbu.avalon.oauth2.common.properties

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Duration

class Oauth2PropertiesTest {
    @Test
    fun defaultsArePopulatedAndDurationParsed() {
        val properties = Oauth2Properties()

        assertIterableEquals(emptyList<String>(), properties.ignoreUrls)
        assertEquals("username", properties.usernameParameterName)
        assertEquals("password", properties.passwordParameterName)
        assertEquals(5, properties.maxLoginFailures)
        assertEquals(Duration.ofMinutes(15), properties.getLoginLockDuration())
    }

    @Test
    fun durationParsingSupportsNumericString() {
        val properties = Oauth2Properties()

        properties.setLoginLockDuration("30")

        assertEquals(Duration.ofMinutes(30), properties.getLoginLockDuration())
    }

    @Test
    fun durationParsingReturnsNullForBlankOrInvalid() {
        val properties = Oauth2Properties()
        properties.setLoginLockDuration("   ")
        assertNull(properties.getLoginLockDuration())

        properties.setLoginLockDuration("not-a-duration")
        assertNull(properties.getLoginLockDuration())

        properties.setLoginLockDuration(null)
        assertNull(properties.getLoginLockDuration())
    }

    @Test
    fun settersUpdateValues() {
        val properties = Oauth2Properties()

        properties.ignoreUrls = mutableListOf("/health", "/docs")
        properties.usernameParameterName = "user"
        properties.passwordParameterName = "pwd"
        properties.maxLoginFailures = 10
        properties.issuerUrl = "https://issuer"
        properties.jwtPublicKeyLocation = "classpath:pub.pem"
        properties.jwtPrivateKeyLocation = "classpath:pri.pem"

        assertIterableEquals(listOf("/health", "/docs"), properties.ignoreUrls)
        assertEquals("user", properties.usernameParameterName)
        assertEquals("pwd", properties.passwordParameterName)
        assertEquals(10, properties.maxLoginFailures)
        assertEquals("https://issuer", properties.issuerUrl)
        assertEquals("classpath:pub.pem", properties.jwtPublicKeyLocation)
        assertEquals("classpath:pri.pem", properties.jwtPrivateKeyLocation)
    }
}
