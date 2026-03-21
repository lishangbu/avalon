package io.github.lishangbu.avalon.oauth2.authorizationserver.web.authentication

import io.github.lishangbu.avalon.oauth2.common.log.AuthenticationLogRecorder
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jackson.autoconfigure.JacksonAutoConfiguration
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.util.ReflectionTestUtils
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import java.nio.charset.StandardCharsets
import java.util.*

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [JacksonAutoConfiguration::class])
class OAuth2ErrorApiResultAuthenticationFailureHandlerTest {
    @Autowired
    private lateinit var jsonMapper: JsonMapper

    @Test
    fun writesDescriptionForOAuth2Error() {
        val handler =
            OAuth2ErrorApiResultAuthenticationFailureHandler(
                AuthenticationLogRecorder.noop(),
                Oauth2Properties(),
                jsonMapper,
            )
        val exception =
            OAuth2AuthenticationException(OAuth2Error("invalid_request", "detail", null))

        val request = MockHttpServletRequest()
        request.setParameter("username", "user")
        val response = MockHttpServletResponse()

        handler.onAuthenticationFailure(request, response, exception)

        val body: JsonNode = jsonMapper.readTree(response.contentAsString)
        assertEquals("detail", body["errorMessage"].asText())
    }

    @Test
    fun sanitizesInvalidGrantDescriptions() {
        val handler =
            OAuth2ErrorApiResultAuthenticationFailureHandler(
                AuthenticationLogRecorder.noop(),
                null,
                jsonMapper,
            )
        val exception =
            OAuth2AuthenticationException(
                OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "invalid_grant: bad", null),
            )

        val response = MockHttpServletResponse()
        handler.onAuthenticationFailure(MockHttpServletRequest(), response, exception)

        val body: JsonNode = jsonMapper.readTree(response.contentAsString)
        assertEquals("bad", body["errorMessage"].asText())

        val plainInvalidGrant =
            OAuth2AuthenticationException(
                OAuth2Error(OAuth2ErrorCodes.INVALID_GRANT, "invalid_grant", null),
            )
        val response2 = MockHttpServletResponse()
        handler.onAuthenticationFailure(MockHttpServletRequest(), response2, plainInvalidGrant)

        val body2: JsonNode = jsonMapper.readTree(response2.contentAsString)
        assertEquals("Bad Request", body2["errorMessage"].asText())
    }

    @Test
    fun handlesNonOAuth2ExceptionAndExtractsClientId() {
        val handler =
            OAuth2ErrorApiResultAuthenticationFailureHandler(
                AuthenticationLogRecorder.noop(),
                null,
                jsonMapper,
            )

        val request = MockHttpServletRequest()
        val credentials =
            Base64.getEncoder().encodeToString("client:secret".toByteArray(StandardCharsets.UTF_8))
        request.addHeader("Authorization", "Basic $credentials")
        request.remoteAddr = "127.0.0.1"
        val response = MockHttpServletResponse()

        handler.onAuthenticationFailure(
            request,
            response,
            BadCredentialsException("Bad credentials"),
        )

        val body: JsonNode = jsonMapper.readTree(response.contentAsString)
        assertEquals("Bad credentials", body["errorMessage"].asText())
    }

    @Test
    fun resolveHelperMethodsCoverBranches() {
        val handler =
            OAuth2ErrorApiResultAuthenticationFailureHandler(
                AuthenticationLogRecorder.noop(),
                null,
                jsonMapper,
            )

        assertNull(
            ReflectionTestUtils.invokeMethod<Any>(handler, "sanitizeDescription", "invalid_grant"),
        )
        assertEquals(
            "reason",
            ReflectionTestUtils.invokeMethod<String>(
                handler,
                "sanitizeDescription",
                "[invalid_grant] reason",
            ),
        )
        assertEquals(
            "reason",
            ReflectionTestUtils.invokeMethod<String>(
                handler,
                "sanitizeDescription",
                "invalid_grant:reason",
            ),
        )
        assertEquals(
            "reason",
            ReflectionTestUtils.invokeMethod<String>(
                handler,
                "sanitizeDescription",
                "invalid_grantreason",
            ),
        )
        assertEquals(
            "detail",
            ReflectionTestUtils.invokeMethod<String>(handler, "sanitizeDescription", "detail"),
        )
        assertEquals(
            "username",
            ReflectionTestUtils.invokeMethod<String>(handler, "resolveUsernameParameterName"),
        )
        val resolveErrorMethod =
            handler.javaClass.getDeclaredMethod("resolveError", AuthenticationException::class.java)
        resolveErrorMethod.isAccessible = true
        resolveErrorMethod.invoke(handler, null)

        val request = MockHttpServletRequest()
        request.setParameter("client_id", "param-client")
        assertEquals(
            "param-client",
            ReflectionTestUtils.invokeMethod<String>(handler, "resolveClientId", request),
        )

        val badHeaderRequest = MockHttpServletRequest()
        badHeaderRequest.addHeader("Authorization", "Basic ***")
        assertNull(
            ReflectionTestUtils.invokeMethod<Any>(handler, "resolveClientId", badHeaderRequest),
        )

        val emptyHeaderRequest = MockHttpServletRequest()
        emptyHeaderRequest.addHeader("Authorization", "Basic ")
        assertNull(
            ReflectionTestUtils.invokeMethod<Any>(handler, "resolveClientId", emptyHeaderRequest),
        )

        val noColonRequest = MockHttpServletRequest()
        val noColon =
            Base64.getEncoder().encodeToString("clientsecret".toByteArray(StandardCharsets.UTF_8))
        noColonRequest.addHeader("Authorization", "Basic $noColon")
        assertNull(
            ReflectionTestUtils.invokeMethod<Any>(handler, "resolveClientId", noColonRequest),
        )

        val forwarded = MockHttpServletRequest()
        forwarded.addHeader("X-Forwarded-For", "1.1.1.1, 2.2.2.2")
        assertEquals(
            "1.1.1.1",
            ReflectionTestUtils.invokeMethod<String>(handler, "resolveClientIp", forwarded),
        )

        val realIp = MockHttpServletRequest()
        realIp.addHeader("X-Real-IP", "9.9.9.9")
        assertEquals(
            "9.9.9.9",
            ReflectionTestUtils.invokeMethod<String>(handler, "resolveClientIp", realIp),
        )

        val remoteAddr = MockHttpServletRequest()
        remoteAddr.remoteAddr = "127.0.0.1"
        assertEquals(
            "127.0.0.1",
            ReflectionTestUtils.invokeMethod<String>(handler, "resolveClientIp", remoteAddr),
        )

        val properties = Oauth2Properties()
        properties.usernameParameterName = "   "
        val handlerWithProps =
            OAuth2ErrorApiResultAuthenticationFailureHandler(
                AuthenticationLogRecorder.noop(),
                properties,
                jsonMapper,
            )
        assertEquals(
            "username",
            ReflectionTestUtils.invokeMethod<String>(
                handlerWithProps,
                "resolveUsernameParameterName",
            ),
        )

        val response = MockHttpServletResponse()
        val writeFailedResponseMethod =
            handler.javaClass.getDeclaredMethod(
                "writeFailedResponse",
                jakarta.servlet.http.HttpServletResponse::class.java,
                String::class.java,
                String::class.java,
            )
        writeFailedResponseMethod.isAccessible = true
        writeFailedResponseMethod.invoke(handler, response, "custom", null)
        assertNotNull(response.contentAsString)
    }
}
