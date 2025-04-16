package io.github.lishangbu.avalon.security.autoconfiguration;

import io.github.lishangbu.avalon.security.filter.AuthTokenFilter;
import io.github.lishangbu.avalon.security.properties.SecurityProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * web安全配置
 *
 * @author lishangbu
 * @since 2025/4/8
 */
@AutoConfiguration
@EnableWebSecurity
@EnableMethodSecurity
public class WebSecurityAutoConfiguration {

  private final AuthTokenFilter authTokenFilter;

  private final SecurityProperties securityProperties;

  private final AuthenticationProvider authenticationProvider;

  public WebSecurityAutoConfiguration(
      AuthTokenFilter authTokenFilter,
      SecurityProperties securityProperties,
      AuthenticationProvider authenticationProvider) {
    this.authTokenFilter = authTokenFilter;
    this.securityProperties = securityProperties;
    this.authenticationProvider = authenticationProvider;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(
            session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    http.authenticationProvider(authenticationProvider);

    // 放行配置文件中的地址
    for (String ignoreUrl : securityProperties.getIgnoreUrls()) {
      http.authorizeHttpRequests(auth -> auth.requestMatchers(ignoreUrl).permitAll());
    }
    // 其余地址需要认证
    http.authorizeHttpRequests(auth -> auth.anyRequest().authenticated());
    http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }
}
