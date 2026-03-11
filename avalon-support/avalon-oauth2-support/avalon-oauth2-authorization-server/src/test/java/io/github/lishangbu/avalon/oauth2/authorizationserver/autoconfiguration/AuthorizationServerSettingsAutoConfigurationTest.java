package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

class AuthorizationServerSettingsAutoConfigurationTest {

    @Test
    void usesIssuerWhenAbsolute() {
        AuthorizationServerSettingsAutoConfiguration configuration =
                new AuthorizationServerSettingsAutoConfiguration();
        Oauth2Properties properties = new Oauth2Properties();
        properties.setIssuerUrl("https://issuer.example");

        AuthorizationServerSettings settings =
                configuration.authorizationServerSettings(properties);

        assertEquals("https://issuer.example", settings.getIssuer());
    }

    @Test
    void ignoresNonAbsoluteIssuer() {
        AuthorizationServerSettingsAutoConfiguration configuration =
                new AuthorizationServerSettingsAutoConfiguration();
        Oauth2Properties properties = new Oauth2Properties();
        properties.setIssuerUrl("/issuer");

        AuthorizationServerSettings settings =
                configuration.authorizationServerSettings(properties);

        assertNull(settings.getIssuer());
    }
}
