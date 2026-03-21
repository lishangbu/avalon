package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration

import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class AuthorizationServerSettingsAutoConfigurationTest {
    @Test
    fun usesIssuerWhenAbsolute() {
        val configuration = AuthorizationServerSettingsAutoConfiguration()
        val properties = Oauth2Properties()
        properties.issuerUrl = "https://issuer.example"

        val settings = configuration.authorizationServerSettings(properties)

        assertEquals("https://issuer.example", settings.issuer)
    }

    @Test
    fun ignoresNonAbsoluteIssuer() {
        val configuration = AuthorizationServerSettingsAutoConfiguration()
        val properties = Oauth2Properties()
        properties.issuerUrl = "/issuer"

        val settings = configuration.authorizationServerSettings(properties)

        assertNull(settings.issuer)
    }
}
