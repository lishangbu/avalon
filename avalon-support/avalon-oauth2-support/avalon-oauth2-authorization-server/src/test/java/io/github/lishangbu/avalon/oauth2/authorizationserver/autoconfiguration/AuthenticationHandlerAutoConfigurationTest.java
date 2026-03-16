package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.lishangbu.avalon.oauth2.authorizationserver.login.InMemoryLoginFailureTracker;
import io.github.lishangbu.avalon.oauth2.authorizationserver.login.LoginFailureTracker;
import io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication.OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler;
import io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication.OAuth2ErrorApiResultAuthenticationFailureHandler;
import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecorder;
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties;
import java.util.Collections;
import java.util.Iterator;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import tools.jackson.databind.json.JsonMapper;

class AuthenticationHandlerAutoConfigurationTest {

    @Test
    void createsLoginFailureTrackerWithDefaults() {
        AuthenticationHandlerAutoConfiguration configuration =
                new AuthenticationHandlerAutoConfiguration();

        LoginFailureTracker tracker = configuration.loginFailureTracker(providerOf(null));

        assertTrue(tracker instanceof InMemoryLoginFailureTracker);
        assertFalse(tracker.isEnabled());
    }

    @Test
    void createsHandlersUsingProvidedDependencies() {
        AuthenticationHandlerAutoConfiguration configuration =
                new AuthenticationHandlerAutoConfiguration();
        AuthenticationLogRecorder recorder = record -> {};
        OAuth2AuthorizationService authorizationService =
                Mockito.mock(OAuth2AuthorizationService.class);
        Oauth2Properties properties = new Oauth2Properties();
        JsonMapper jsonMapper = new JsonMapper();

        OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler successHandler =
                configuration.accessTokenResponseAuthenticationSuccessHandler(
                        providerOf(recorder),
                        providerOf(authorizationService),
                        providerOf(properties),
                        jsonMapper);
        OAuth2ErrorApiResultAuthenticationFailureHandler failureHandler =
                configuration.oauth2ErrorApiResultAuthenticationFailureHandler(
                        providerOf(recorder), providerOf(properties), jsonMapper);

        assertNotNull(successHandler);
        assertNotNull(failureHandler);
    }

    private static <T> ObjectProvider<T> providerOf(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject() {
                return value;
            }

            @Override
            public T getObject(Object... args) {
                return value;
            }

            @Override
            public T getIfAvailable() {
                return value;
            }

            @Override
            public T getIfAvailable(Supplier<T> supplier) {
                return value != null ? value : supplier.get();
            }

            @Override
            public Iterator<T> iterator() {
                return value == null
                        ? Collections.emptyIterator()
                        : Collections.singleton(value).iterator();
            }
        };
    }
}
