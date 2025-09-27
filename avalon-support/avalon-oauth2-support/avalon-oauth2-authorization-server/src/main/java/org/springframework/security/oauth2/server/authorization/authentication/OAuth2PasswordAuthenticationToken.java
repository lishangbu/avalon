package org.springframework.security.oauth2.server.authorization.authentication;

import java.io.Serial;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.util.Assert;

/**
 * 表示 OAuth2 密码模式（Resource Owner Password Credentials Grant）的认证令牌
 *
 * <p>封装密码模式认证所需的基本信息，包括用户名、凭证、额外参数和用户信息
 *
 * <p>该令牌用于认证流程中在 AuthenticationManager 与 AuthenticationProvider 之间传递认证数据 继承自 {@link
 * AbstractAuthenticationToken}，可以携带权限集合用于后续权限校验
 *
 * @author xuxiaowei
 * @author lishangbu
 * @see OAuth2AuthorizationConsentAuthenticationToken
 * @see OAuth2AccessTokenAuthenticationToken
 * @see OAuth2ClientAuthenticationToken
 * @see SecurityJackson2Modules
 * @since 2025/9/28
 */
public class OAuth2PasswordAuthenticationToken extends AbstractAuthenticationToken {

  @Serial private static final long serialVersionUID = 1L;

  /**
   * 认证主体的用户名，作为认证的 principal
   *
   * <p>不可为空，构造时会进行非空校验
   */
  private final String username;

  /**
   * 认证凭证，通常为用户明文密码或经过保护的凭证
   *
   * <p>在认证成功或不需要凭证时可能为 null
   */
  private final Object credentials;

  /**
   * 请求中的额外参数集合，包含除标准 OAuth2 参数外的自定义参数
   *
   * <p>构造后为不可变视图，避免外部修改
   */
  @Getter private final Map<String, Object> additionalParameters;

  /**
   * 构造一个新的 OAuth2PasswordAuthenticationToken 实例
   *
   * @param username 用户名，不能为空
   * @param credentials 认证凭证，通常为密码，可为 null
   * @param authorities 权限集合，可为 null
   * @param additionalParameters 额外参数集合，允许为 null，构造后将转换为不可变集合
   */
  public OAuth2PasswordAuthenticationToken(
      String username,
      @Nullable Object credentials,
      @Nullable Collection<? extends GrantedAuthority> authorities,
      @Nullable Map<String, Object> additionalParameters) {
    super(authorities);
    Assert.hasText(username, "username cannot be empty");
    this.username = username;
    this.credentials = credentials;
    this.additionalParameters =
        Collections.unmodifiableMap(
            (additionalParameters != null) ? additionalParameters : Collections.emptyMap());
  }

  /**
   * 返回认证凭证对象
   *
   * <p>该凭证通常为用户提交的密码或其它认证凭证
   *
   * @return 认证凭证，可能为 null
   */
  @Override
  public Object getCredentials() {
    return this.credentials;
  }

  /**
   * 返回认证主体（principal），这里为用户名
   *
   * @return 用户名，永远不为 null
   */
  @Override
  public Object getPrincipal() {
    return this.username;
  }
}
