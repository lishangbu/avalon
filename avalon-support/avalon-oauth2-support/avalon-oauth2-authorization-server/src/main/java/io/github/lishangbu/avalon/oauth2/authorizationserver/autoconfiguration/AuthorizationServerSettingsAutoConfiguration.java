package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration;

import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.web.util.UrlUtils;

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
  public AuthorizationServerSettings authorizationServerSettings(Oauth2Properties properties) {
    AuthorizationServerSettings.Builder builder = AuthorizationServerSettings.builder();
    String issuerUrl = properties.getIssuerUrl();
    if (UrlUtils.isAbsoluteUrl(issuerUrl)) {
      builder.issuer(issuerUrl);
    }
    return builder.build();
  }
}
