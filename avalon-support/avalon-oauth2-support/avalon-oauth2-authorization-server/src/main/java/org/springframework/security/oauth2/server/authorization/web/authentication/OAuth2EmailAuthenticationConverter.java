package org.springframework.security.oauth2.server.authorization.web.authentication;

import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport;
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2EmailAuthorizationGrantAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/// OAuth2 邮箱授权类型认证转换器
///
/// 将使用 `grant_type=email` 的 HTTP 表单请求转换为
/// `OAuth2EmailAuthorizationGrantAuthenticationToken`，并负责解析与验证参数
///
/// @author lishangbu
/// @since 2026/3/13
@RequiredArgsConstructor
public class OAuth2EmailAuthenticationConverter implements AuthenticationConverter {

    /// 邮箱授权类型请求参数错误时的 RFC 文档参考链接（Extension Grants）
    static final String EMAIL_REQUEST_ERROR_URI =
            "https://datatracker.ietf.org/doc/html/rfc6749#section-4.5";

    private final Oauth2Properties oauth2Properties;

    @Nullable
    @Override
    public Authentication convert(HttpServletRequest request) {
        MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getFormParameters(request);

        // grant_type (REQUIRED)
        String grantType = parameters.getFirst(OAuth2ParameterNames.GRANT_TYPE);
        if (!AuthorizationGrantTypeSupport.EMAIL.getValue().equals(grantType)) {
            return null;
        }

        Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();

        final String emailParameterName = oauth2Properties.getEmailParameterName();
        final String email = parameters.getFirst(emailParameterName);
        if (!StringUtils.hasText(email) || parameters.get(emailParameterName).size() != 1) {
            OAuth2EndpointUtils.throwError(
                    OAuth2ErrorCodes.INVALID_REQUEST, emailParameterName, EMAIL_REQUEST_ERROR_URI);
        }

        final String emailCodeParameterName = oauth2Properties.getEmailCodeParameterName();
        final String emailCode = parameters.getFirst(emailCodeParameterName);
        if (!StringUtils.hasText(emailCode) || parameters.get(emailCodeParameterName).size() != 1) {
            OAuth2EndpointUtils.throwError(
                    OAuth2ErrorCodes.INVALID_REQUEST,
                    emailCodeParameterName,
                    EMAIL_REQUEST_ERROR_URI);
        }

        // scope (OPTIONAL)
        String scope = parameters.getFirst(OAuth2ParameterNames.SCOPE);
        if (StringUtils.hasText(scope) && parameters.get(OAuth2ParameterNames.SCOPE).size() != 1) {
            OAuth2EndpointUtils.throwError(
                    OAuth2ErrorCodes.INVALID_REQUEST,
                    OAuth2ParameterNames.SCOPE,
                    EMAIL_REQUEST_ERROR_URI);
        }
        Set<String> requestedScopes = null;
        if (StringUtils.hasText(scope)) {
            requestedScopes =
                    new HashSet<>(
                            Arrays.asList(StringUtils.delimitedListToStringArray(scope, " ")));
        }

        Map<String, Object> additionalParameters = new HashMap<>();
        parameters.forEach(
                (key, value) -> {
                    if (!key.equals(OAuth2ParameterNames.GRANT_TYPE)
                            && !key.equals(OAuth2ParameterNames.SCOPE)
                            && !key.equals(OAuth2ParameterNames.CLIENT_ID)
                            && !key.equals(OAuth2ParameterNames.CLIENT_SECRET)
                            && !key.equals(emailParameterName)
                            && !key.equals(emailCodeParameterName)) {
                        additionalParameters.put(
                                key,
                                (value.size() == 1)
                                        ? value.getFirst()
                                        : value.toArray(new String[0]));
                    }
                });

        return new OAuth2EmailAuthorizationGrantAuthenticationToken(
                email, emailCode, clientPrincipal, requestedScopes, additionalParameters);
    }
}
