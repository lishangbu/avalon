package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;

/**
 * A facility for authorization server configuration settings.
 *
 * @author lishangbu
 * @since 2025/8/17
 */
@AutoConfiguration
public class AuthorizationServerSettingsAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public AuthorizationServerSettings authorizationServerSettings() {

    return AuthorizationServerSettings.builder().build();
  }
}
