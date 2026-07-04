package io.github.lishangbu.security.config

import io.github.lishangbu.security.oauth.PasswordGrantAuthenticationConverter
import io.github.lishangbu.security.oauth.PasswordGrantAuthenticationProvider
import io.github.lishangbu.security.oauth.BearerTokenAuthenticationManagerResolver
import io.github.lishangbu.security.oauth.OpaqueTokenAuthenticationProvider
import io.github.lishangbu.security.oauth.securityAuthoritiesFromClaims
import io.github.lishangbu.security.rbac.BATTLE_RULES_ADMIN_ACCESS_NODE
import io.github.lishangbu.security.rbac.BATTLE_SANDBOX_RUN_ACCESS_NODE
import io.github.lishangbu.security.rbac.GAME_DATA_ADMIN_ACCESS_NODE
import io.github.lishangbu.security.rbac.JimmerSecurityUserDetailsService
import io.github.lishangbu.security.rbac.SECURITY_ADMIN_ACCESS_NODE
import org.babyfish.jimmer.spring.repository.EnableJimmerRepositories
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configurers.oauth2.server.authorization.OAuth2AuthorizationServerConfigurer
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.http.HttpMethod
import org.springframework.security.oauth2.core.OAuth2Token
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.SecurityFilterChain

/**
 * Backend 安全模块的主配置。
 *
 * 启用安全后同时装配授权服务器、资源服务器和 API 权限规则。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
@EnableJimmerRepositories(basePackages = ["io.github.lishangbu.security.repository"])
class SecurityConfig {
	/**
	 * 使用 Spring Security 推荐的委托式密码编码器，保留算法前缀以支持后续平滑升级。
	 */
	@Bean
	fun passwordEncoder(): PasswordEncoder =
		PasswordEncoderFactories.createDelegatingPasswordEncoder()

	/**
	 * 装配用户名密码认证管理器，供自定义 password grant 复用同一套用户校验逻辑。
	 */
	@Bean
	fun authenticationManager(
		userDetailsService: JimmerSecurityUserDetailsService,
		passwordEncoder: PasswordEncoder,
	): AuthenticationManager {
		val authenticationProvider = DaoAuthenticationProvider(userDetailsService)
		authenticationProvider.setPasswordEncoder(passwordEncoder)
		return ProviderManager(authenticationProvider)
	}

	/**
	 * 授权服务器对外声明的 issuer 配置。
	 */
	@Bean
	fun authorizationServerSettings(properties: SecurityProperties): AuthorizationServerSettings =
		AuthorizationServerSettings.builder()
			.issuer(properties.issuer)
			.build()

	/**
	 * 授权服务器端点安全链。
	 *
	 * 在标准授权服务器能力上挂载 Backend 自定义 password grant。
	 */
	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE)
	fun authorizationServerSecurityFilterChain(
		http: HttpSecurity,
		registeredClientRepository: RegisteredClientRepository,
		authorizationService: OAuth2AuthorizationService,
		authorizationConsentService: OAuth2AuthorizationConsentService,
		authorizationServerSettings: AuthorizationServerSettings,
		tokenGenerator: OAuth2TokenGenerator<OAuth2Token>,
		authenticationManager: AuthenticationManager,
	): SecurityFilterChain {
		val authorizationServerConfigurer = OAuth2AuthorizationServerConfigurer()
		http
			.securityMatcher(authorizationServerConfigurer.endpointsMatcher)
			.with(authorizationServerConfigurer) { authorizationServer ->
				authorizationServer
					.registeredClientRepository(registeredClientRepository)
					.authorizationService(authorizationService)
					.authorizationConsentService(authorizationConsentService)
					.authorizationServerSettings(authorizationServerSettings)
					.tokenGenerator(tokenGenerator)
					.tokenEndpoint { tokenEndpoint ->
						tokenEndpoint
							.accessTokenRequestConverter(PasswordGrantAuthenticationConverter())
							.authenticationProvider(
								PasswordGrantAuthenticationProvider(
									authenticationManager = authenticationManager,
									authorizationService = authorizationService,
									tokenGenerator = tokenGenerator,
								),
							)
					}
			}
			.authorizeHttpRequests { authorize ->
				authorize.anyRequest().authenticated()
			}
			.csrf { csrf ->
				csrf.ignoringRequestMatchers(authorizationServerConfigurer.endpointsMatcher)
			}
		return http.build()
	}

	/**
	 * JWT bearer token 认证提供者。
	 *
	 * 自定义 converter 用于把 Backend 权限和角色 claim 转成 Spring Security authority。
	 */
	@Bean
	fun jwtAuthenticationProvider(jwtDecoder: JwtDecoder): JwtAuthenticationProvider {
		val authenticationProvider = JwtAuthenticationProvider(jwtDecoder)
		authenticationProvider.setJwtAuthenticationConverter { jwt ->
			JwtAuthenticationToken(jwt, securityAuthoritiesFromClaims(jwt.claims), jwt.subject ?: jwt.tokenValue)
		}
		return authenticationProvider
	}

	/**
	 * reference token 认证提供者。
	 */
	@Bean
	fun opaqueAuthenticationProvider(
		authorizationService: OAuth2AuthorizationService,
	): OpaqueTokenAuthenticationProvider =
		OpaqueTokenAuthenticationProvider(authorizationService)

	/**
	 * 根据 token 形态在 JWT 和 reference token 认证之间分流。
	 */
	@Bean
	fun bearerTokenAuthenticationManagerResolver(
		jwtAuthenticationProvider: JwtAuthenticationProvider,
		opaqueAuthenticationProvider: OpaqueTokenAuthenticationProvider,
	): BearerTokenAuthenticationManagerResolver =
		BearerTokenAuthenticationManagerResolver(jwtAuthenticationProvider, opaqueAuthenticationProvider)

	/**
	 * OpenAPI 文档端点权限链。
	 *
	 * 文档端点需要在启用安全后仍可供本地联调和前端类型生成工具读取；实际业务 API
	 * 仍由后续 API 权限链按 Bearer token 和 RBAC 权限强制校验。
	 */
	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE + 1)
	fun openApiSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
		http
			.securityMatcher("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
			.authorizeHttpRequests { authorize ->
				authorize.anyRequest().permitAll()
			}
			.csrf { csrf ->
				csrf.disable()
			}
		return http.build()
	}

	/**
	 * 后端 API 权限链。
	 */
	@Bean
	@Order(Ordered.HIGHEST_PRECEDENCE + 2)
	fun apiSecurityFilterChain(
		http: HttpSecurity,
		authenticationManagerResolver: BearerTokenAuthenticationManagerResolver,
	): SecurityFilterChain {
		http
			.securityMatcher("/api/**")
			.authorizeHttpRequests { authorize ->
				authorize
					.requestMatchers(HttpMethod.OPTIONS, "/api/**").permitAll()
					// 管理端路由权限在后端强制校验，前端只负责改善交互体验。
					.requestMatchers("/api/system/**").hasAuthority(SECURITY_ADMIN_ACCESS_NODE)
					.requestMatchers("/api/battle-rules/**").hasAuthority(BATTLE_RULES_ADMIN_ACCESS_NODE)
					.requestMatchers("/api/battle-sandbox/**").hasAuthority(BATTLE_SANDBOX_RUN_ACCESS_NODE)
					.requestMatchers("/api/game-data/**").hasAuthority(GAME_DATA_ADMIN_ACCESS_NODE)
					.anyRequest().authenticated()
			}
			.oauth2ResourceServer { resourceServer ->
				resourceServer.authenticationManagerResolver(authenticationManagerResolver)
			}
			.csrf { csrf ->
				csrf.disable()
			}
		return http.build()
	}
}
