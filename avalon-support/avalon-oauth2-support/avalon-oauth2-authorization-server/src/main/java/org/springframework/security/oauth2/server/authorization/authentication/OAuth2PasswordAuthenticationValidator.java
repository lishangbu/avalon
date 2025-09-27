package org.springframework.security.oauth2.server.authorization.authentication;

import java.util.Set;
import java.util.function.Consumer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.log.LogMessage;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;

/**
 * OAuth 2.0 Resource Owner Password Credentials Grant 认证请求验证器
 *
 * <p>负责校验 password 授权类型下的请求约束，包括 scope 的合法性校验
 *
 * <p>设计要点：
 *
 * <ul>
 *   <li>将具体的校验逻辑抽象为 Consumer 接口，便于替换或扩展校验策略
 *   <li>默认校验仅针对 scope 是否被注册客户端允许
 * </ul>
 *
 * @author xuxiaowei
 * @author lishangbu
 * @since 2025/9/28
 */
public class OAuth2PasswordAuthenticationValidator
    implements Consumer<OAuth2PasswordAuthenticationContext> {

  private static final Log LOGGER = LogFactory.getLog(OAuth2PasswordAuthenticationValidator.class);

  /**
   * 默认的 scope 校验器
   *
   * <p>该校验器用于验证客户端请求的 scope 是否均包含在注册客户端允许的 scope 列表中
   *
   * <p>使用场景：当没有自定义校验器时，使用此默认实现进行 scope 合法性校验
   */
  public static final Consumer<OAuth2PasswordAuthenticationContext> DEFAULT_SCOPE_VALIDATOR =
      OAuth2PasswordAuthenticationValidator::validateScope;

  /**
   * 实际用于执行校验的 Consumer 实例
   *
   * <p>默认为 {@link #DEFAULT_SCOPE_VALIDATOR}，可以通过替换该字段实现自定义校验逻辑
   */
  private final Consumer<OAuth2PasswordAuthenticationContext> authenticationValidator =
      DEFAULT_SCOPE_VALIDATOR;

  /**
   * 执行对 {@link OAuth2PasswordAuthenticationContext} 的校验
   *
   * <p>本方法会将上下文传递给内部配置的校验器进行校验，校验失败将抛出相应的认证异常
   *
   * @param authenticationContext 待校验的认证上下文，包含请求的认证令牌和注册客户端信息
   * @throws OAuth2AuthenticationException 如果校验失败将抛出相应的异常
   */
  @Override
  public void accept(OAuth2PasswordAuthenticationContext authenticationContext) {
    this.authenticationValidator.accept(authenticationContext);
  }

  /**
   * 校验请求的 scope 是否被注册客户端允许
   *
   * <p>逻辑要点：
   *
   * <ul>
   *   <li>从认证上下文中取出请求的 scope 和注册客户端允许的 scope
   *   <li>如果请求 scope 非空且不属于注册客户端允许的范围，则记录日志并抛出 {@link OAuth2AuthenticationException}
   * </ul>
   *
   * @param authenticationContext 包含 {@link OAuth2PasswordAuthorizationGrantAuthenticationToken} 和
   *     {@link RegisteredClient} 的上下文
   * @throws OAuth2AuthenticationException 当请求的 scope 超出注册客户端允许范围时抛出，错误码为 {@link
   *     OAuth2ErrorCodes#INVALID_SCOPE}
   */
  private static void validateScope(OAuth2PasswordAuthenticationContext authenticationContext) {
    OAuth2PasswordAuthorizationGrantAuthenticationToken passwordGrantAuthenticationToken =
        authenticationContext.getAuthentication();
    RegisteredClient registeredClient = authenticationContext.getRegisteredClient();

    Set<String> requestedScopes = passwordGrantAuthenticationToken.getScopes();
    Set<String> allowedScopes = registeredClient.getScopes();
    if (!requestedScopes.isEmpty() && !allowedScopes.containsAll(requestedScopes)) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug(
            LogMessage.format(
                "Invalid request: requested scope is not allowed" + " for registered client '%s'",
                registeredClient.getId()));
      }
      throw new OAuth2AuthenticationException(OAuth2ErrorCodes.INVALID_SCOPE);
    }
  }
}
