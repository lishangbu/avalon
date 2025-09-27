package org.springframework.security.oauth2.server.authorization.authentication;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.util.Assert;

/**
 * Password 授权模式认证上下文
 *
 * @author xuxiaowei
 * @author lishangbu
 * @since 2025/9/28
 */
public class OAuth2PasswordAuthenticationContext implements OAuth2AuthenticationContext {

  private final Map<Object, Object> context;

  private OAuth2PasswordAuthenticationContext(Map<Object, Object> context) {
    this.context = Collections.unmodifiableMap(new HashMap<>(context));
  }

  @SuppressWarnings("unchecked")
  @Override
  public <V> V get(Object key) {
    return hasKey(key) ? (V) this.context.get(key) : null;
  }

  @Override
  public boolean hasKey(Object key) {
    Assert.notNull(key, "key cannot be null");
    return this.context.containsKey(key);
  }

  /**
   * Returns the {@link RegisteredClient registered client}.
   *
   * @return the {@link RegisteredClient}
   */
  public RegisteredClient getRegisteredClient() {
    return get(RegisteredClient.class);
  }

  /**
   * Constructs a new {@link OAuth2PasswordAuthenticationContext.Builder} with the provided {@link
   * OAuth2PasswordAuthorizationGrantAuthenticationToken}.
   *
   * @param authentication the {@link OAuth2PasswordAuthorizationGrantAuthenticationToken}
   * @return the {@link OAuth2PasswordAuthenticationContext.Builder}
   */
  public static OAuth2PasswordAuthenticationContext.Builder with(
      OAuth2PasswordAuthorizationGrantAuthenticationToken authentication) {
    return new OAuth2PasswordAuthenticationContext.Builder(authentication);
  }

  /** A builder for {@link OAuth2PasswordAuthenticationContext}. */
  public static final class Builder
      extends OAuth2AuthenticationContext.AbstractBuilder<
          OAuth2PasswordAuthenticationContext, Builder> {

    private Builder(OAuth2PasswordAuthorizationGrantAuthenticationToken authentication) {
      super(authentication);
    }

    /**
     * Sets the {@link RegisteredClient registered client}.
     *
     * @param registeredClient the {@link RegisteredClient}
     * @return the {@link OAuth2PasswordAuthenticationContext.Builder} for further configuration
     */
    public OAuth2PasswordAuthenticationContext.Builder registeredClient(
        RegisteredClient registeredClient) {
      return put(RegisteredClient.class, registeredClient);
    }

    /**
     * Builds a new {@link OAuth2PasswordAuthenticationContext}.
     *
     * @return the {@link OAuth2PasswordAuthenticationContext}
     */
    @Override
    public OAuth2PasswordAuthenticationContext build() {
      Assert.notNull(get(RegisteredClient.class), "registeredClient cannot be null");
      return new OAuth2PasswordAuthenticationContext(getContext());
    }
  }
}
