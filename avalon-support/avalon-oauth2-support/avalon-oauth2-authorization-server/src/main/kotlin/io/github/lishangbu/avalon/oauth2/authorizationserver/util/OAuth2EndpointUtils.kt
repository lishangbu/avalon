package io.github.lishangbu.avalon.oauth2.authorizationserver.util

import jakarta.servlet.http.HttpServletRequest
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.OAuth2AuthenticationException
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.security.oauth2.core.endpoint.PkceParameterNames
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap

object OAuth2EndpointUtils {
    const val ACCESS_TOKEN_REQUEST_ERROR_URI =
        "https://datatracker.ietf.org/doc/html/rfc6749#section-5.2"

    @JvmStatic
    fun getParameters(request: HttpServletRequest): MultiValueMap<String, String> {
        val parameterMap = request.parameterMap
        val parameters = LinkedMultiValueMap<String, String>(parameterMap.size)
        parameterMap.forEach { (key, values) ->
            values.forEach { value -> parameters.add(key, value) }
        }
        return parameters
    }

    @JvmStatic
    fun getFormParameters(request: HttpServletRequest): MultiValueMap<String, String> = getParameters(request)

    @JvmStatic
    fun matchesPkceTokenRequest(request: HttpServletRequest): Boolean =
        AuthorizationGrantType.AUTHORIZATION_CODE.value ==
            request.getParameter(OAuth2ParameterNames.GRANT_TYPE) &&
            request.getParameter(OAuth2ParameterNames.CODE) != null &&
            request.getParameter(PkceParameterNames.CODE_VERIFIER) != null

    @JvmStatic
    fun throwError(
        errorCode: String,
        parameterName: String,
        errorUri: String,
    ) {
        val error = OAuth2Error(errorCode, "OAuth 2.0 Parameter: $parameterName", errorUri)
        throw OAuth2AuthenticationException(error)
    }
}
