package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration;

import static io.github.lishangbu.avalon.oauth2.common.constant.SecurityBeanDefinitionConstants.AUTHORIZATION_SERVER_SECURITY_FILTER_CHAIN_BEAN_NAME;
import static io.github.lishangbu.avalon.oauth2.common.constant.SecurityBeanDefinitionConstants.AUTHORIZATION_SERVER_SECURITY_FILTER_CHAIN_BEAN_ORDER;

import io.github.lishangbu.avalon.oauth2.authorizationserver.granter.OAuth2PasswordAuthenticationConverter;
import io.github.lishangbu.avalon.oauth2.authorizationserver.granter.OAuth2PasswordAuthenticationProvider;
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties;
import io.github.lishangbu.avalon.oauth2.common.web.authentication.DefaultAuthenticationEntryPoint;
import java.util.Arrays;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.*;
import org.springframework.security.oauth2.core.OAuth2Token;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2AuthorizationCodeAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2ClientCredentialsAuthenticationConverter;
import org.springframework.security.oauth2.server.authorization.web.authentication.OAuth2RefreshTokenAuthenticationConverter;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.DelegatingAuthenticationConverter;
import org.springframework.security.web.util.matcher.MediaTypeRequestMatcher;

/**
 * 自动装配认证服务器
 *
 * @author lishangbu
 * @since 2025/8/17
 */
@EnableWebSecurity
@AutoConfiguration
@EnableMethodSecurity(jsr250Enabled = true, securedEnabled = true)
@RequiredArgsConstructor
public class AuthorizationServerAutoConfiguration {

  private final Oauth2Properties oauth2Properties;

  @Bean
  @Order(AUTHORIZATION_SERVER_SECURITY_FILTER_CHAIN_BEAN_ORDER)
  @ConditionalOnMissingBean(name = AUTHORIZATION_SERVER_SECURITY_FILTER_CHAIN_BEAN_NAME)
  public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http)
      throws Exception {
    OAuth2AuthorizationServerConfigurer authorizationServerConfigurer =
        OAuth2AuthorizationServerConfigurer.authorizationServer();
    // 禁用csrf和cors
    http.csrf(CsrfConfigurer::disable)
        .cors(CorsConfigurer::disable)
        // 禁用表单登录和会话管理
        .formLogin(FormLoginConfigurer::disable)
        .sessionManagement(SessionManagementConfigurer::disable)
        .rememberMe(RememberMeConfigurer::disable);

    DefaultSecurityFilterChain securityFilterChain =
        http.securityMatcher(authorizationServerConfigurer.getEndpointsMatcher())
            .with(
                authorizationServerConfigurer,
                (authorizationServer) ->
                    // Enable OpenID Connect 1.0
                    authorizationServer
                        .oidc(Customizer.withDefaults())
                        // 在这加上密码模式的转换器
                        .tokenEndpoint(
                            tokenEndpoint ->
                                tokenEndpoint.accessTokenRequestConverter(
                                    new DelegatingAuthenticationConverter(
                                        Arrays.asList(
                                            new OAuth2AuthorizationCodeAuthenticationConverter(),
                                            new OAuth2RefreshTokenAuthenticationConverter(),
                                            new OAuth2ClientCredentialsAuthenticationConverter(),
                                            new OAuth2PasswordAuthenticationConverter())))))
            .authorizeHttpRequests((authorize) -> authorize.anyRequest().authenticated())
            // Redirect to the login page when not authenticated from the
            // authorization endpoint
            .exceptionHandling(
                exceptions -> {
                  exceptions.defaultAuthenticationEntryPointFor(
                      new DefaultAuthenticationEntryPoint(),
                      new MediaTypeRequestMatcher(MediaType.APPLICATION_JSON, MediaType.TEXT_HTML));
                })
            .build();

    addPasswordAuthenticationProvider(http);
    return securityFilterChain;
  }

  @SuppressWarnings("unchecked")
  private void addPasswordAuthenticationProvider(HttpSecurity http) {
    AuthenticationManager authenticationManager = http.getSharedObject(AuthenticationManager.class);
    OAuth2AuthorizationService authorizationService =
        http.getSharedObject(OAuth2AuthorizationService.class);
    OAuth2TokenGenerator<? extends OAuth2Token> tokenGenerator =
        http.getSharedObject(OAuth2TokenGenerator.class);

    OAuth2PasswordAuthenticationProvider resourceOwnerPasswordAuthenticationProvider =
        new OAuth2PasswordAuthenticationProvider(
            authenticationManager, authorizationService, tokenGenerator);

    // This will add new authentication provider in the list of existing authentication providers.
    http.authenticationProvider(resourceOwnerPasswordAuthenticationProvider);
  }
}
