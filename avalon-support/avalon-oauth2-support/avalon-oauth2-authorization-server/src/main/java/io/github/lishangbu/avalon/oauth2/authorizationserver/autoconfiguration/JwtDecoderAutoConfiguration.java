package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * JwtDecoder 自动装配
 *
 * @author lishangbu
 * @since 2025/8/17
 */
@AutoConfiguration
public class JwtDecoderAutoConfiguration {
  /**
   * An instance of JwtDecoder for decoding signed access tokens.
   *
   * @param jwkSource
   * @return
   */
  @Bean
  @ConditionalOnMissingBean
  public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
    return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
  }
}
