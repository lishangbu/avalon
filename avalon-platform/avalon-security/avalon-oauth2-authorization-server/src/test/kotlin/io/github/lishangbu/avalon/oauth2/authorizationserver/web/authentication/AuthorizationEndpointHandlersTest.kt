package io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.ClientAuthenticationMethod
import org.springframework.security.oauth2.core.OAuth2AccessToken
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2RefreshToken
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.time.Instant

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [JacksonAutoConfiguration::class])
class AuthorizationEndpointHandlersTest {
    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @Test
    fun authorizationEndpointErrorResponseWritesApiResult() {
        val handler = AuthorizationEndpointErrorResponseHandler(jsonMapper)
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()

        handler.onAuthenticationFailure(
            request,
            response,
            OAuth2AuthenticationException("unauthorized"),
        )

        assertEquals(401, response.status)
        val body: JsonNode = jsonMapper.readTree(response.contentAsString)
        assertEquals("unauthorized", body["errorMessage"].asText())
    }

    @Test
    fun authorizationEndpointResponseHandlerWritesTokenResponse() {
        val handler = AuthorizationEndpointResponseHandler()
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val authentication = accessTokenAuthentication()

        handler.onAuthenticationSuccess(request, response, authentication)

        assertEquals(200, response.status)
        val body = response.contentAsString
        assertTrue(body.contains("access_token"))
        assertTrue(body.contains("token_type"))
    }

    @Test
    fun authorizationEndpointResponseHandlerAppliesCustomizer() {
        val handler = AuthorizationEndpointResponseHandler()
        handler.setAccessTokenResponseCustomizer { context ->
            context.accessTokenResponse.additionalParameters(mapOf("custom" to "value"))
        }

        val response = MockHttpServletResponse()
        handler.onAuthenticationSuccess(
            MockHttpServletRequest(),
            response,
            accessTokenAuthentication(),
        )

        val body: JsonNode = jsonMapper.readTree(response.contentAsString)
        val custom = body["custom"]
        assertNotNull(custom)
        assertEquals("value", custom.asText())
    }

    @Test
    fun authorizationEndpointResponseHandlerRejectsInvalidAuthenticationType() {
        val handler = AuthorizationEndpointResponseHandler()

        assertThrows(OAuth2AuthenticationException::class.java) {
            handler.onAuthenticationSuccess(
                MockHttpServletRequest(),
                MockHttpServletResponse(),
                UsernamePasswordAuthenticationToken("user", "pwd"),
            )
        }
    }

    @Test
    fun setAccessTokenResponseCustomizerRequiresNonNull() {
        val handler = AuthorizationEndpointResponseHandler()
        val method =
            AuthorizationEndpointResponseHandler::class
                .java
                .getMethod(
                    "setAccessTokenResponseCustomizer",
                    java.util.function.Consumer::class.java,
                )

        assertThrows(Exception::class.java) { method.invoke(handler, null) }
    }

    companion object {
        private fun accessTokenAuthentication(): OAuth2AccessTokenAuthenticationToken {
            val registeredClient =
                RegisteredClient
                    .withId("id")
                    .clientId("client")
                    .clientSecret("secret")
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(
                        org.springframework.security.oauth2.core
                            .AuthorizationGrantType("password"),
                    ).scope("read")
                    .tokenSettings(TokenSettings.builder().build())
                    .build()
            val clientAuthentication: Authentication =
                OAuth2ClientAuthenticationToken(
                    registeredClient,
                    ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                    "secret",
                )
            val accessToken =
                OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    "token",
                    Instant.now(),
                    Instant.now().plusSeconds(60),
                    setOf("read"),
                )
            val refreshToken =
                OAuth2RefreshToken("refresh", Instant.now(), Instant.now().plusSeconds(120))
            return OAuth2AccessTokenAuthenticationToken(
                registeredClient,
                clientAuthentication,
                accessToken,
                refreshToken,
                mapOf(),
            )
        }
    }
}
