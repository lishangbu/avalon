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
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.*;
import org.springframework.security.web.SecurityFilterChain;

/**
 * 资源服务器自动配置类
 *
 * <p>为资源服务器提供默认的 Spring Security `SecurityFilterChain` 配置
 *
 * <p>核心职责：
 *
 * <ul>
 *   <li>配置受保护资源的请求匹配规则与认证要求
 *   <li>禁用不必要的默认功能（如 csrf、formLogin、sessionManagement 等）以支持无状态的 API 场景
 *   <li>为资源服务器设置统一的认证入口点和访问拒绝处理器，确保认证/鉴权失败时返回统一的 JSON 响应格式
 *   <li>集成透明令牌（opaque token）验证逻辑，并确保当令牌无效时触发统一的异常处理
 * </ul>
 *
 * 配置要点：
 *
 * <ul>
 *   <li>使用 `@Order` 指定该 `SecurityFilterChain` 的优先级，避免与认证服务器链冲突
 *   <li>优先调用 `oauth2ResourceServer(...)` 并在其内部显式设置 `authenticationEntryPoint`，以防资源服务器默认行为覆盖自定义的入口点
 *   <li>通过 `http.exceptionHandling(...)` 提供全局的兜底处理器
 * </ul>
 *
 * 注意：本配置在缺省情况下会被自动装配，若项目中存在同名 bean（`resourceServerSecurityFilterChain`）则不会覆盖
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

  /**
   * OAuth2 相关配置属性
   *
   * <p>主要用于获取需要忽略认证的 URL 列表（例如静态资源、开放接口等）
   */
  private final Oauth2Properties oauth2Properties;

  /**
   * 资源服务器的安全过滤链
   *
   * <p>配置说明：
   *
   * <ol>
   *   <li>放行 `oauth2Properties.ignoreUrls` 中配置的 URL；其余请求都要求认证
   *   <li>禁用 CSRF、CORS、表单登录、会话管理和记住我功能，以支持无状态 REST API 场景
   *   <li>先配置 `oauth2ResourceServer(...)` 并显式设置 `opaqueToken` 与
   *       `authenticationEntryPoint`，避免内置默认覆盖自定义入口点
   *   <li>随后配置全局 `exceptionHandling`，设置统一的 `AuthenticationEntryPoint` 和
   *       `AccessDeniedHandler`，用于处理未认证或无权限访问的情况并返回统一 JSON 响应格式
   * </ol>
   *
   * @param http HttpSecurity 构造器，用于构建 SecurityFilterChain
   * @return 构建完成的 SecurityFilterChain
   * @throws Exception 当构建 SecurityFilterChain 失败时抛出
   */
  @Bean
  @ConditionalOnMissingBean(name = RESOURCE_SERVER_SECURITY_FILTER_CHAIN_BEAN_NAME)
  @Order(RESOURCE_SERVER_SECURITY_FILTER_CHAIN_BEAN_ORDER)
  public SecurityFilterChain resourceServerSecurityFilterChain(HttpSecurity http) throws Exception {
    http.authorizeHttpRequests(
            (authorize) ->
                authorize
                    // 放行静态资源和不需要认证的url
                    .requestMatchers(oauth2Properties.getIgnoreUrls().toArray(new String[0]))
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        // 禁用csrf和cors
        .csrf(CsrfConfigurer::disable)
        .cors(CorsConfigurer::disable)
        // 禁用表单登录和会话管理
        .formLogin(FormLoginConfigurer::disable)
        .sessionManagement(SessionManagementConfigurer::disable)
        .rememberMe(RememberMeConfigurer::disable);

    // 注意：
    // `oauth2ResourceServer(...)` 可能会在其应用后安装自己的异常处理（包括 AuthenticationEntryPoint）
    // 为避免覆盖我们自定义的 AuthenticationEntryPoint，请先配置资源服务器，然后再设置 exceptionHandling
    // 这样可以保证使用我们配置的统一入口点来处理认证失败或令牌无效的情况
    http.oauth2ResourceServer(
        oauth2ResourceServer -> {
          oauth2ResourceServer.opaqueToken(Customizer.withDefaults());
          // 确保资源服务器在 token 校验失败时也使用我们的统一 entry point
          oauth2ResourceServer
              .authenticationEntryPoint(new DefaultAuthenticationEntryPoint())
              .accessDeniedHandler(new DefaultAccessDeniedHandler());
        });

    // 在 oauth2ResourceServer 之后配置异常处理，确保我们的处理器不会被资源服务器的默认实现覆盖
    http.exceptionHandling(
        exceptions ->
            exceptions
                .authenticationEntryPoint(new DefaultAuthenticationEntryPoint())
                .accessDeniedHandler(new DefaultAccessDeniedHandler()));

    return http.build();
  }
}
