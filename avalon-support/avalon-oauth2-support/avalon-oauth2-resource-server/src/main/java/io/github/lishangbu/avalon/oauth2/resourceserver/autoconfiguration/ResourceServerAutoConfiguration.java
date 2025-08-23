package io.github.lishangbu.avalon.oauth2.resourceserver.autoconfiguration;

import static io.github.lishangbu.avalon.oauth2.common.constant.SecurityBeanDefinitionConstants.RESOURCE_SERVER_SECURITY_FILTER_CHAIN_BEAN_NAME;
import static io.github.lishangbu.avalon.oauth2.common.constant.SecurityBeanDefinitionConstants.RESOURCE_SERVER_SECURITY_FILTER_CHAIN_BEAN_ORDER;

import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties;
import io.github.lishangbu.avalon.oauth2.common.web.access.DefaultAccessDeniedHandler;
import io.github.lishangbu.avalon.oauth2.common.web.authentication.DefaultAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.*;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/**
 * 自动装配认证服务器
 *
 * @author lishangbu
 * @since 2025/8/17
 */
@Slf4j
@EnableWebSecurity
@AutoConfiguration
@EnableMethodSecurity(jsr250Enabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class ResourceServerAutoConfiguration {

  private final Oauth2Properties oauth2Properties;

  @Bean
  @ConditionalOnMissingBean(name = RESOURCE_SERVER_SECURITY_FILTER_CHAIN_BEAN_NAME)
  @Order(RESOURCE_SERVER_SECURITY_FILTER_CHAIN_BEAN_ORDER)
  public SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated())
        // 禁用csrf和cors
        .csrf(CsrfConfigurer::disable)
        .cors(CorsConfigurer::disable)
        // 禁用表单登录和会话管理
        .formLogin(FormLoginConfigurer::disable)
        .sessionManagement(SessionManagementConfigurer::disable)
        .rememberMe(RememberMeConfigurer::disable)
        .exceptionHandling(
            exceptions -> {
              exceptions.defaultAuthenticationEntryPointFor(
                  new DefaultAuthenticationEntryPoint(),
                  new MediaTypeRequestMatcher(MediaType.APPLICATION_JSON, MediaType.TEXT_HTML));
            });
    ;

    http.oauth2ResourceServer(
        oauth2ResourceServer -> {
          oauth2ResourceServer
              .opaqueToken(Customizer.withDefaults())
              .accessDeniedHandler(new DefaultAccessDeniedHandler());
        });

    return http.build();
  }
}
