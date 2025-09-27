package org.springframework.security.oauth2.server.authorization.web.authentication;

import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport;
import io.github.lishangbu.avalon.oauth2.common.core.endpoint.OAuth2PasswordParameterNames;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2PasswordAuthorizationGrantAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationConverter;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

/**
 * 将使用 {@code grant_type=password} 的 HTTP 表单请求转换为 {@link
 * OAuth2PasswordAuthorizationGrantAuthenticationToken}
 *
 * <p>该转换器负责从请求中解析并校验 password 授权类型所需的参数，包括 username、password、scope 等
 *
 * <p>主要职责：
 *
 * <ul>
 *   <li>仅处理 grant_type 为 password 的请求
 *   <li>从 Spring Security 上下文获取已认证的客户端信息并作为 clientPrincipal 传入生成的认证令牌
 *   <li>校验必需参数的存在性和重复性，非法时调用 {@link OAuth2EndpointUtils#throwError(String, String, String)}
 *       抛出标准错误响应
 *   <li>解析 scope 为集合形式，并收集除标准参数外的额外参数传递给认证令牌
 * </ul>
 *
 * @author xuxiaowei
 * @author lishangbu
 * @see OAuth2AuthorizationCodeAuthenticationConverter
 * @see OAuth2RefreshTokenAuthenticationConverter
 * @see OAuth2ClientCredentialsAuthenticationConverter
 * @since 2025/9/29
 */
public class OAuth2PasswordAuthenticationConverter implements AuthenticationConverter {

  /**
   * 当请求参数不合法时参考的 RFC 文档 URI
   *
   * <p>用于在调用 OAuth2EndpointUtils.throwError 时传递错误描述的参考链接
   */
  static final String PASSWORD_REQUEST_ERROR_URI =
      "https://datatracker.ietf.org/doc/html/rfc6749#section-4.3.2";

  /**
   * 将 HTTP 请求转换为 OAuth2 password 授权类型的认证令牌
   *
   * <p>该方法会解析请求体中的表单参数并校验以下项：
   *
   * <ul>
   *   <li>grant_type 必须为 {@code password}，否则返回 null 表示不处理该请求
   *   <li>username 和 password 为必需参数，且各自只能出现一次，非法时通过 {@link OAuth2EndpointUtils#throwError(String,
   *       String, String)} 抛出 {@code OAuth2AuthenticationException}
   *   <li>scope 为可选参数，如果存在且合法则解析为字符串集合
   *   <li>收集除标准参数外的额外参数并传入生成的认证令牌
   * </ul>
   *
   * @param request 当前 HTTP 请求，必须为 application/x-www-form-urlencoded 的 token 请求
   * @return 如果请求的 grant_type 不是 password 返回 null，若为 password 返回构建好的 {@link
   *     OAuth2PasswordAuthorizationGrantAuthenticationToken}
   * @throws org.springframework.security.oauth2.core.OAuth2AuthenticationException
   *     当请求参数缺失或重复导致无效请求时抛出
   */
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

    String username = parameters.getFirst(OAuth2PasswordParameterNames.USERNAME);
    if (!StringUtils.hasText(username)
        || parameters.get(OAuth2PasswordParameterNames.USERNAME).size() != 1) {
      OAuth2EndpointUtils.throwError(
          OAuth2ErrorCodes.INVALID_REQUEST,
          OAuth2PasswordParameterNames.USERNAME,
          PASSWORD_REQUEST_ERROR_URI);
    }

    String password = parameters.getFirst(OAuth2PasswordParameterNames.PASSWORD);
    if (!StringUtils.hasText(password)
        || parameters.get(OAuth2PasswordParameterNames.PASSWORD).size() != 1) {
      OAuth2EndpointUtils.throwError(
          OAuth2ErrorCodes.INVALID_REQUEST,
          OAuth2PasswordParameterNames.PASSWORD,
          PASSWORD_REQUEST_ERROR_URI);
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
              && !key.equals(OAuth2PasswordParameterNames.USERNAME)
              && !key.equals(OAuth2PasswordParameterNames.PASSWORD)) {
            additionalParameters.put(
                key, (value.size() == 1) ? value.get(0) : value.toArray(new String[0]));
          }
        });

    return new OAuth2PasswordAuthorizationGrantAuthenticationToken(
        username, password, clientPrincipal, requestedScopes, additionalParameters);
  }
}
