package io.github.lishangbu.avalon.oauth2.authorizationserver.token;

import io.github.lishangbu.avalon.oauth2.authorizationserver.keygen.UuidKeyGenerator;
import java.time.Instant;
import org.springframework.lang.Nullable;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.core.OAuth2RefreshToken;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

/// 自定义 RefreshToken 生成器
///
/// 无论何种授权方式，均返回一个基于 UUID 的 refresh token
///
/// @author lishangbu
/// @since 2025/8/22
public class OAuth2RefreshTokenGenerator implements OAuth2TokenGenerator<OAuth2RefreshToken> {

  private final StringKeyGenerator refreshTokenGenerator = new UuidKeyGenerator();

  @Nullable
  @Override
  public OAuth2RefreshToken generate(OAuth2TokenContext context) {
    // @formatter:off
    if (!OAuth2TokenType.REFRESH_TOKEN.equals(context.getTokenType())) {
      return null;
    }
    // @formatter:on
    Instant issuedAt = Instant.now();
    Instant expiresAt =
        issuedAt.plus(context.getRegisteredClient().getTokenSettings().getRefreshTokenTimeToLive());
    return new OAuth2RefreshToken(this.refreshTokenGenerator.generateKey(), issuedAt, expiresAt);
  }
}
