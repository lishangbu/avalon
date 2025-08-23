package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration;

import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * JwtEncoder 自动装配
 *
 * @author lishangbu
 * @since 2025/8/22
 */
@AutoConfiguration
public class JwtEncoderAutoConfiguration {
  @Bean
  @ConditionalOnMissingBean
  public JwtEncoder jwtEncoder(JWKSource<SecurityContext> jwkSource) {
    return new NimbusJwtEncoder(jwkSource);
  }
}
