package io.github.lishangbu.avalon.oauth2.common.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class Oauth2PropertiesTest {

    @Test
    void defaultsArePopulatedAndDurationParsed() {
        Oauth2Properties properties = new Oauth2Properties();

        assertIterableEquals(List.of(), properties.getIgnoreUrls());
        assertEquals("username", properties.getUsernameParameterName());
        assertEquals("password", properties.getPasswordParameterName());
        assertEquals(5, properties.getMaxLoginFailures());
        assertEquals(Duration.ofMinutes(15), properties.getLoginLockDuration());
    }

    @Test
    void durationParsingSupportsNumericString() {
        Oauth2Properties properties = new Oauth2Properties();

        properties.setLoginLockDuration("30");

        assertEquals(Duration.ofMinutes(30), properties.getLoginLockDuration());
    }

    @Test
    void durationParsingReturnsNullForBlankOrInvalid() {
        Oauth2Properties properties = new Oauth2Properties();
        properties.setLoginLockDuration("   ");
        assertNull(properties.getLoginLockDuration());

        properties.setLoginLockDuration("not-a-duration");
        assertNull(properties.getLoginLockDuration());

        properties.setLoginLockDuration(null);
        assertNull(properties.getLoginLockDuration());
    }

    @Test
    void settersUpdateValues() {
        Oauth2Properties properties = new Oauth2Properties();

        properties.setIgnoreUrls(List.of("/health", "/docs"));
        properties.setUsernameParameterName("user");
        properties.setPasswordParameterName("pwd");
        properties.setMaxLoginFailures(10);
        properties.setIssuerUrl("https://issuer");
        properties.setJwtPublicKeyLocation("classpath:pub.pem");
        properties.setJwtPrivateKeyLocation("classpath:pri.pem");

        assertIterableEquals(List.of("/health", "/docs"), properties.getIgnoreUrls());
        assertEquals("user", properties.getUsernameParameterName());
        assertEquals("pwd", properties.getPasswordParameterName());
        assertEquals(10, properties.getMaxLoginFailures());
        assertEquals("https://issuer", properties.getIssuerUrl());
        assertEquals("classpath:pub.pem", properties.getJwtPublicKeyLocation());
        assertEquals("classpath:pri.pem", properties.getJwtPrivateKeyLocation());
    }
}
