package io.github.lishangbu.avalon.oauth2.authorizationserver.introspection;

import io.github.lishangbu.avalon.oauth2.common.userdetails.UserInfo;
import java.security.Principal;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.DefaultOAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;

/// 默认透明令牌处理器
///
/// 用于资源服务器在收到不透明 access token 时执行 introspect，并根据授权与用户信息返回相应的 Principal
///
/// @author lishangbu
/// @since 2025/8/22
@Slf4j
@RequiredArgsConstructor
public class DefaultOpaqueTokenIntrospector implements OpaqueTokenIntrospector {
  /// OAuth2 授权服务
  private final OAuth2AuthorizationService authorizationService;

  private final UserDetailsService userDetailsService;

  @Override
  public OAuth2AuthenticatedPrincipal introspect(String token) {
    OAuth2Authorization oldAuthorization =
        authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
    if (Objects.isNull(oldAuthorization)) {
      throw new InvalidBearerTokenException(token);
    }
    // 客户端模式默认返回
    if (AuthorizationGrantType.CLIENT_CREDENTIALS.equals(
        oldAuthorization.getAuthorizationGrantType())) {
      return new DefaultOAuth2AuthenticatedPrincipal(
          oldAuthorization.getPrincipalName(),
          Objects.requireNonNull(oldAuthorization.getAccessToken().getClaims()),
          AuthorityUtils.NO_AUTHORITIES);
    }

    UserDetails userDetails = null;
    try {
      Object principal =
          Objects.requireNonNull(oldAuthorization).getAttributes().get(Principal.class.getName());
      UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken =
          (UsernamePasswordAuthenticationToken) principal;
      Object tokenPrincipal = usernamePasswordAuthenticationToken.getPrincipal();
      userDetails =
          userDetailsService.loadUserByUsername(((UserDetails) tokenPrincipal).getUsername());
      if (userDetails instanceof UserInfo userInfo) {
        userInfo.getAttributes().putAll(oldAuthorization.getAccessToken().getClaims());
        return userInfo;
      }
    } catch (UsernameNotFoundException notFoundException) {
      log.warn("用户不不存在 {}", notFoundException.getLocalizedMessage());
      throw notFoundException;
    } catch (Exception ex) {
      log.error("资源服务器 introspect Token error {}", ex.getLocalizedMessage());
    }

    return null;
  }
}
