package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration;

import io.github.lishangbu.avalon.oauth2.authorizationserver.token.OAuth2RefreshTokenGenerator;
import io.github.lishangbu.avalon.oauth2.authorizationserver.token.ReferenceOAuth2AccessTokenGenerator;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;

/**
 * x
 *
 * @author lishangbu
 * @since 2025/8/22
 */
@AutoConfiguration
@RequiredArgsConstructor
public class OAuth2TokenGeneratorAutoConfiguration {
  private final JwtEncoder jwtEncoder;

  @Bean
  public OAuth2TokenGenerator<?> tokenGenerator() {
    return new DelegatingOAuth2TokenGenerator(
        // reference的token生成器
        new ReferenceOAuth2AccessTokenGenerator(),
        // reference的refreshToken生成器
        new OAuth2RefreshTokenGenerator(),
        new JwtGenerator(jwtEncoder));
  }
}
