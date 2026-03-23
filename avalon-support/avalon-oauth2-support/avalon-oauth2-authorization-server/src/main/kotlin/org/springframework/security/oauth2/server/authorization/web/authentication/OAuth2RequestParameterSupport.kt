package org.springframework.security.oauth2.server.authorization.web.authentication

import io.github.lishangbu.avalon.oauth2.authorizationserver.util.OAuth2EndpointUtils
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.util.MultiValueMap
import org.springframework.util.StringUtils

internal fun MultiValueMap<String, String>.requireSingleTextParameter(
    parameterName: String,
    errorUri: String,
): String =
    get(parameterName)?.singleOrNull()?.takeIf(StringUtils::hasText)
        ?: OAuth2EndpointUtils.throwError(
            OAuth2ErrorCodes.INVALID_REQUEST,
            parameterName,
            errorUri,
        )

internal fun MultiValueMap<String, String>.readRequestedScopes(errorUri: String): Set<String>? {
    val scope = getFirst(OAuth2ParameterNames.SCOPE)
    if (StringUtils.hasText(scope) && this[OAuth2ParameterNames.SCOPE]?.size != 1) {
        OAuth2EndpointUtils.throwError(
            OAuth2ErrorCodes.INVALID_REQUEST,
            OAuth2ParameterNames.SCOPE,
            errorUri,
        )
    }
    return scope
        ?.takeIf(StringUtils::hasText)
        ?.let { LinkedHashSet(StringUtils.delimitedListToStringArray(it, " ").toList()) }
}
