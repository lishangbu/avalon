package io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication

import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecord
import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecorder
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.*
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationContext
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2AccessTokenAuthenticationToken
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.util.ReflectionTestUtils
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.security.Principal
import java.time.Instant
import java.util.function.Consumer

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [JacksonAutoConfiguration::class])
class OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandlerTest {
    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @Test
    fun throwsWhenAuthenticationTypeIsInvalid() {
        val handler =
            OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler(jsonMapper = jsonMapper)

        assertThrows(OAuth2AuthenticationException::class.java) {
            handler.onAuthenticationSuccess(
                MockHttpServletRequest(),
                MockHttpServletResponse(),
                UsernamePasswordAuthenticationToken("user", "pwd"),
            )
        }
    }

    @Test
    fun writesApiResultAndRecordsLog() {
        val recorder = CapturingRecorder()
        val handler =
            OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler(
                recorder,
                null,
                Oauth2Properties(),
                jsonMapper,
            )

        val request = MockHttpServletRequest()
        request.setParameter(OAuth2ParameterNames.GRANT_TYPE, "password")
        request.setParameter("username", "user")
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2")
        request.addHeader("User-Agent", "JUnit")

        val response = MockHttpServletResponse()
        handler.onAuthenticationSuccess(request, response, accessTokenAuthentication(mapOf()))

        assertEquals(200, response.status)
        val body = response.contentAsString
        assertTrue(body.contains("access_token"))

        val record = recorder.record
        assertNotNull(record)
        assertEquals("user", record!!.username)
        assertEquals("client", record.clientId)
        assertEquals("password", record.grantType)
        assertEquals("10.0.0.1", record.ip)
        assertEquals("JUnit", record.userAgent)
    }

    @Test
    fun usesAdditionalParametersAndAuthorizationServiceForUsername() {
        val recorder = CapturingRecorder()
        val authorizationService = Mockito.mock(OAuth2AuthorizationService::class.java)
        val authorization =
            OAuth2Authorization
                .withRegisteredClient(registeredClient())
                .principalName("auth-user")
                .authorizationGrantType(
                    org.springframework.security.oauth2.core
                        .AuthorizationGrantType("password"),
                ).build()
        Mockito
            .`when`(authorizationService.findByToken("token", OAuth2TokenType.ACCESS_TOKEN))
            .thenReturn(authorization)

        val handler =
            OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler(
                recorder,
                authorizationService,
                null,
                jsonMapper,
            )

        val request = MockHttpServletRequest()
        request.addHeader("X-Real-IP", "192.168.0.1")

        val additionalParameters = HashMap<String, Any>()
        additionalParameters[OAuth2ParameterNames.GRANT_TYPE] = "password"

        handler.onAuthenticationSuccess(
            request,
            MockHttpServletResponse(),
            accessTokenAuthentication(additionalParameters),
        )

        val record = recorder.record
        assertEquals("auth-user", record?.username)
        assertEquals("192.168.0.1", record?.ip)
    }

    @Test
    fun handlesRecorderFailureGracefully() {
        val handler =
            OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler(
                { throw IllegalStateException("boom") },
                null,
                null,
                jsonMapper,
            )

        val response = MockHttpServletResponse()
        handler.onAuthenticationSuccess(
            MockHttpServletRequest(),
            response,
            accessTokenAuthentication(mapOf()),
        )

        assertEquals(200, response.status)
    }

    @Test
    fun appliesAccessTokenResponseCustomizer() {
        val handler =
            OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler(jsonMapper = jsonMapper)
        ReflectionTestUtils.setField(
            handler,
            "accessTokenResponseCustomizer",
            Consumer<OAuth2AccessTokenAuthenticationContext> { context ->
                context.accessTokenResponse.additionalParameters(mapOf("custom" to "value"))
            },
        )

        val response = MockHttpServletResponse()
        handler.onAuthenticationSuccess(
            MockHttpServletRequest(),
            response,
            accessTokenAuthentication(mapOf()),
        )

        val body: JsonNode = jsonMapper.readTree(response.contentAsString)
        assertEquals("value", body["data"]["custom"].asText())
    }

    @Test
    fun omitsRefreshTokenWhenAuthenticationDoesNotContainOne() {
        val handler =
            OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler(jsonMapper = jsonMapper)
        val response = MockHttpServletResponse()

        handler.onAuthenticationSuccess(
            MockHttpServletRequest(),
            response,
            accessTokenAuthentication(mapOf(), refreshToken = null),
        )

        val body: JsonNode = jsonMapper.readTree(response.contentAsString)
        assertNull(body["data"]["refresh_token"])
    }

    @Test
    fun resolveHelperMethodsCoverBranches() {
        val handler =
            OAuth2AccessTokenApiResultResponseAuthenticationSuccessHandler(
                AuthenticationLogRecorder.noop(),
                null,
                null,
                jsonMapper,
            )
        val request = MockHttpServletRequest()
        request.addHeader("X-Forwarded-For", "1.1.1.1, 2.2.2.2")
        request.addHeader("X-Real-IP", "2.2.2.2")
        request.remoteAddr = "127.0.0.1"

        val clientIp = ReflectionTestUtils.invokeMethod<String>(handler, "resolveClientIp", request)
        assertEquals("1.1.1.1", clientIp)

        val realIpRequest = MockHttpServletRequest()
        realIpRequest.addHeader("X-Real-IP", "10.10.10.10")
        realIpRequest.remoteAddr = "127.0.0.1"
        assertEquals(
            "10.10.10.10",
            ReflectionTestUtils.invokeMethod<String>(handler, "resolveClientIp", realIpRequest),
        )

        val remoteAddrRequest = MockHttpServletRequest()
        remoteAddrRequest.remoteAddr = "127.0.0.1"
        assertEquals(
            "127.0.0.1",
            ReflectionTestUtils.invokeMethod<String>(handler, "resolveClientIp", remoteAddrRequest),
        )

        val grantType =
            ReflectionTestUtils.invokeMethod<String>(
                handler,
                "resolveGrantType",
                remoteAddrRequest,
                mapOf<String, Any>(),
            )
        assertNull(grantType)
        assertEquals(
            "username",
            ReflectionTestUtils.invokeMethod<String>(handler, "resolveUsernameParameterName"),
        )

        val principal =
            object {
                override fun toString(): String = "principal"
            }
        val authentication = accessTokenAuthentication(mapOf())

        val clientCredentialsUsername =
            ReflectionTestUtils.invokeMethod<String>(
                handler,
                "resolveUsername",
                principal,
                remoteAddrRequest,
                authentication,
                AuthorizationGrantType.CLIENT_CREDENTIALS.value,
            )
        assertNull(clientCredentialsUsername)

        var resolved =
            ReflectionTestUtils.invokeMethod<String>(
                handler,
                "resolveUsername",
                principal,
                remoteAddrRequest,
                authentication,
                "password",
            )
        assertEquals("principal", resolved)

        val genericPrincipal = Principal { "generic" }
        resolved =
            ReflectionTestUtils.invokeMethod(
                handler,
                "resolveUsername",
                genericPrincipal,
                remoteAddrRequest,
                authentication,
                "password",
            )
        assertEquals("generic", resolved)

        val clientAuthenticationToken =
            OAuth2ClientAuthenticationToken(
                "client",
                ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                "secret",
                mapOf(),
            )
        resolved =
            ReflectionTestUtils.invokeMethod(
                handler,
                "resolveUsername",
                clientAuthenticationToken,
                remoteAddrRequest,
                authentication,
                "password",
            )
        assertNull(resolved)

        val mockToken = Mockito.mock(OAuth2AccessTokenAuthenticationToken::class.java)
        Mockito.`when`(mockToken.registeredClient).thenReturn(null)
        val clientId =
            ReflectionTestUtils.invokeMethod<String>(
                handler,
                "resolveClientId",
                mockToken,
                clientAuthenticationToken,
            )
        assertEquals("client", clientId)
    }

    companion object {
        private fun accessTokenAuthentication(
            additionalParameters: Map<String, Any>,
            refreshToken: OAuth2RefreshToken? =
                OAuth2RefreshToken("refresh", Instant.now(), Instant.now().plusSeconds(120)),
        ): OAuth2AccessTokenAuthenticationToken {
            val accessToken =
                OAuth2AccessToken(
                    OAuth2AccessToken.TokenType.BEARER,
                    "token",
                    Instant.now(),
                    Instant.now().plusSeconds(60),
                    setOf("read"),
                )
            val client = registeredClient()
            val clientAuthentication: Authentication =
                OAuth2ClientAuthenticationToken(
                    client,
                    ClientAuthenticationMethod.CLIENT_SECRET_BASIC,
                    "secret",
                )
            return OAuth2AccessTokenAuthenticationToken(
                client,
                clientAuthentication,
                accessToken,
                refreshToken,
                additionalParameters,
            )
        }

        private fun registeredClient(): RegisteredClient =
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
    }

    private class CapturingRecorder : AuthenticationLogRecorder {
        var record: AuthenticationLogRecord? = null

        override fun record(record: AuthenticationLogRecord) {
            this.record = record
        }
    }
}
