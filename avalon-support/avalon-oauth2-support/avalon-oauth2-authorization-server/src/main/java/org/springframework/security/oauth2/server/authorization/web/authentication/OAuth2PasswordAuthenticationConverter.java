package org.springframework.security.oauth2.server.authorization.web.authentication;

import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport;
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2PasswordAuthorizationGrantAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/// OAuth2 密码授权类型认证转换器
///
/// 将使用 `grant_type=password` 的 HTTP 表单请求转换为
/// `OAuth2PasswordAuthorizationGrantAuthenticationToken`，并负责解析与验证参数
///
/// 主要功能：
///
/// - 仅处理 grant_type 为 password 的授权请求
/// - 从 Spring Security 上下文获取已认证的客户端信息
/// - 验证 username 和 password 参数的有效性和唯一性
/// - 解析可选的 scope 参数并转换为字符串集合
/// - 收集额外的自定义参数传递给认证令牌
///
/// 使用示例与 RFC 说明详见源码注释
///
/// @author lishangbu
/// @see Oauth2Properties
/// @see OAuth2PasswordAuthorizationGrantAuthenticationToken
/// @see OAuth2EndpointUtils
/// @since 2025/9/29
@RequiredArgsConstructor
public class OAuth2PasswordAuthenticationConverter implements AuthenticationConverter {

  /// 密码授权类型请求参数错误时的 RFC 文档参考链接
  /// 指向 RFC 6749 第 4.3.2 节关于密码授权类型的规范说明
  static final String PASSWORD_REQUEST_ERROR_URI =
      "https://datatracker.ietf.org/doc/html/rfc6749#section-4.3.2";
  /// OAuth2 配置属性，用于获取自定义的用户名和密码参数名称
  private final Oauth2Properties oauth2Properties;

  /// 将 HTTP 请求转换为 OAuth2 密码授权类型的认证令牌
  ///
  /// 解析请求体中的表单参数并进行严格验证，确保符合 OAuth2 密码授权类型的规范要求
  ///
  /// @param request 当前 HTTP 请求，必须为 application/x-www-form-urlencoded 格式的 token 请求
  /// @return 如果请求的 grant_type 不是 password 返回 null，否则返回构建好的认证令牌
  /// @throws org.springframework.security.oauth2.core.OAuth2AuthenticationException
  // 当请求参数缺失、重复或格式不正确时抛出
  @Nullable
  @Override
  public Authentication convert(HttpServletRequest request) {
    MultiValueMap<String, String> parameters = OAuth2EndpointUtils.getFormParameters(request);

    // grant_type (REQUIRED)
    String grantType = parameters.getFirst(OAuth2ParameterNames.GRANT_TYPE);
    if (!AuthorizationGrantTypeSupport.PASSWORD.getValue().equals(grantType)) {
      return null;
    }

    Authentication clientPrincipal = SecurityContextHolder.getContext().getAuthentication();

    final String usernameParameterName = oauth2Properties.getUsernameParameterName();
    final String username = parameters.getFirst(usernameParameterName);
    if (!StringUtils.hasText(username) || parameters.get(usernameParameterName).size() != 1) {
      OAuth2EndpointUtils.throwError(
          OAuth2ErrorCodes.INVALID_REQUEST, usernameParameterName, PASSWORD_REQUEST_ERROR_URI);
    }

    final String passwordParameterName = oauth2Properties.getPasswordParameterName();
    final String password = parameters.getFirst(passwordParameterName);
    if (!StringUtils.hasText(password) || parameters.get(passwordParameterName).size() != 1) {
      OAuth2EndpointUtils.throwError(
          OAuth2ErrorCodes.INVALID_REQUEST, passwordParameterName, PASSWORD_REQUEST_ERROR_URI);
    }

    // scope (OPTIONAL)
    String scope = parameters.getFirst(OAuth2ParameterNames.SCOPE);
    if (StringUtils.hasText(scope) && parameters.get(OAuth2ParameterNames.SCOPE).size() != 1) {
      OAuth2EndpointUtils.throwError(
          OAuth2ErrorCodes.INVALID_REQUEST, OAuth2ParameterNames.SCOPE, PASSWORD_REQUEST_ERROR_URI);
    }
    Set<String> requestedScopes = null;
    if (StringUtils.hasText(scope)) {
      requestedScopes =
          new HashSet<>(Arrays.asList(StringUtils.delimitedListToStringArray(scope, " ")));
    }

    Map<String, Object> additionalParameters = new HashMap<>();
    parameters.forEach(
        (key, value) -> {
          if (!key.equals(OAuth2ParameterNames.GRANT_TYPE)
              && !key.equals(OAuth2ParameterNames.SCOPE)
              && !key.equals(OAuth2ParameterNames.CLIENT_ID)
              && !key.equals(OAuth2ParameterNames.CLIENT_SECRET)
              && !key.equals(usernameParameterName)
              && !key.equals(passwordParameterName)) {
            additionalParameters.put(
                key, (value.size() == 1) ? value.getFirst() : value.toArray(new String[0]));
          }
        });

    return new OAuth2PasswordAuthorizationGrantAuthenticationToken(
        username, password, clientPrincipal, requestedScopes, additionalParameters);
  }
}
