package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration;

import io.github.lishangbu.avalon.oauth2.authorizationserver.token.JwtOAuth2TokenCustomizer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

/// OAUTH2 Token 自定义配置
///
/// 提供 JWT Token 自定义处理器的 Bean 定义
///
/// @author lishangbu
/// @since 2025/8/21
@AutoConfiguration
public class OAuth2TokenCustomizerAutoConfiguration {
  /// 自定义 OAuth2 Token Claims 上下文
  /// @return OAuth2TokenCustomizer 的实例
  @Bean
  public OAuth2TokenCustomizer<JwtEncodingContext> oAuth2TokenCustomizer() {
    return new JwtOAuth2TokenCustomizer();
  }
}
