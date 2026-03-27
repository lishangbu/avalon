package org.springframework.security.oauth2.server.authorization.web.authentication

import io.github.lishangbu.avalon.oauth2.authorizationserver.util.OAuth2EndpointUtils
import org.springframework.security.oauth2.core.OAuth2ErrorCodes
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames
import org.springframework.util.MultiValueMap

/** 校验并返回单个文本参数 */
internal fun MultiValueMap<String, String>.requireSingleTextParameter(
    parameterName: String,
    errorUri: String,
): String =
    get(parameterName)?.singleOrNull()?.takeIf(String::isNotBlank)
        ?: OAuth2EndpointUtils.throwError(
            OAuth2ErrorCodes.INVALID_REQUEST,
            parameterName,
            errorUri,
        )

/** 读取请求的权限范围列表 */
internal fun MultiValueMap<String, String>.readRequestedScopes(errorUri: String): Set<String>? {
    val scope = getFirst(OAuth2ParameterNames.SCOPE)
    if (!scope.isNullOrBlank() && this[OAuth2ParameterNames.SCOPE]?.size != 1) {
        OAuth2EndpointUtils.throwError(
            OAuth2ErrorCodes.INVALID_REQUEST,
            OAuth2ParameterNames.SCOPE,
            errorUri,
        )
    }
    return scope
        ?.takeIf(String::isNotBlank)
        ?.trim()
        ?.splitToSequence(' ')
        ?.filter(String::isNotBlank)
        ?.toCollection(linkedSetOf())
}
