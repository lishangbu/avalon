package io.github.lishangbu.avalon.security.autoconfiguration;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 认证管理器自动配置
 *
 * @author lishangbu
 * @since 2025/4/9
 */
@AutoConfiguration
@RequiredArgsConstructor
public class AuthenticationProviderAutoConfiguration {
  private final PasswordEncoder passwordEncoder;

  @Autowired(required = false)
  private UserDetailsService userDetailsService;

  @Bean
  @ConditionalOnBean(UserDetailsService.class)
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder);
    return authProvider;
  }
}
