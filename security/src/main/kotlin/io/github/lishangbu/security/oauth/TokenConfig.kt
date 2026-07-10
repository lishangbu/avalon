package io.github.lishangbu.security.oauth

import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import io.github.lishangbu.security.rbac.BATTLE_RULES_ADMIN_ACCESS_NODE
import io.github.lishangbu.security.rbac.GAME_DATA_ADMIN_ACCESS_NODE
import io.github.lishangbu.security.rbac.SECURITY_ADMIN_ACCESS_NODE
import io.github.lishangbu.security.rbac.SecurityUserPrincipal
import io.github.lishangbu.security.repository.OAuth2AuthorizationConsentRecordRepository
import io.github.lishangbu.security.repository.OAuth2AuthorizationRecordRepository
import io.github.lishangbu.security.repository.OAuth2JwkRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.sql.init.dependency.DependsOnDatabaseInitialization
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.core.Authentication
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration
import org.springframework.security.oauth2.core.OAuth2Token
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationConsentService
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository
import org.springframework.security.oauth2.server.authorization.token.DelegatingOAuth2TokenGenerator
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext
import org.springframework.security.oauth2.server.authorization.token.JwtGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2AccessTokenGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2RefreshTokenGenerator
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenClaimsContext
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenGenerator
import tools.jackson.databind.ObjectMapper

/**
 * 授权服务器 token、JWK 和授权记录相关配置。
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class TokenConfig {
	/**
	 * 使用 Jimmer 映射的标准授权表保存授权记录。
	 */
	@Bean
	fun authorizationService(
		authorizationRepository: OAuth2AuthorizationRecordRepository,
		registeredClientRepository: RegisteredClientRepository,
		sqlClient: KSqlClient,
		objectMapper: ObjectMapper,
	): OAuth2AuthorizationService =
		JimmerOAuth2AuthorizationService(authorizationRepository, registeredClientRepository, sqlClient, objectMapper)

	/**
	 * 使用 Jimmer Repository 保存用户授权同意记录。
	 */
	@Bean
	fun authorizationConsentService(
		consentRepository: OAuth2AuthorizationConsentRecordRepository,
	): OAuth2AuthorizationConsentService =
		JimmerOAuth2AuthorizationConsentService(consentRepository)

	/**
	 * 创建授权服务器签名 key 工厂。
	 */
	@Bean
	fun oauth2JwkKeyFactory(): OAuth2JwkKeyFactory =
		OAuth2JwkKeyFactory()

	/**
	 * 从数据库提供授权服务器签名 key。
	 */
	@Bean
	@DependsOnDatabaseInitialization
	fun jwkSource(
		repository: OAuth2JwkRepository,
		sqlClient: KSqlClient,
		jwkKeyFactory: OAuth2JwkKeyFactory,
	): JWKSource<SecurityContext> =
		JwkSource(repository, sqlClient, jwkKeyFactory)

	/**
	 * 基于同一 JWK 来源创建资源服务器 JWT 解码器。
	 */
	@Bean
	fun jwtDecoder(jwkSource: JWKSource<SecurityContext>): JwtDecoder =
		OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource)

	/**
	 * 同时支持自包含 JWT 和 reference token 的生成器。
	 */
	@Bean
	fun tokenGenerator(
		jwkSource: JWKSource<SecurityContext>,
		jwtCustomizer: OAuth2TokenCustomizer<JwtEncodingContext>,
		opaqueCustomizer: OAuth2TokenCustomizer<OAuth2TokenClaimsContext>,
	): OAuth2TokenGenerator<OAuth2Token> {
		val jwtGenerator = JwtGenerator(NimbusJwtEncoder(jwkSource))
		jwtGenerator.setJwtCustomizer(jwtCustomizer)
		val accessTokenGenerator = OAuth2AccessTokenGenerator()
		accessTokenGenerator.setAccessTokenCustomizer(opaqueCustomizer)
		return DelegatingOAuth2TokenGenerator(
			jwtGenerator,
			accessTokenGenerator,
			OAuth2RefreshTokenGenerator(),
		)
	}

	/**
	 * 为 JWT access token 写入 Backend 权限和角色 claim。
	 */
	@Bean
	fun jwtCustomizer(): OAuth2TokenCustomizer<JwtEncodingContext> =
		OAuth2TokenCustomizer { context ->
			if (context.tokenType == OAuth2TokenType.ACCESS_TOKEN) {
				context.getPrincipal<Authentication>()?.backendPrincipal()?.let { principal ->
					context.claims.claim("access_nodes", principal.scopedAccessNodeCodes(context.authorizedScopes))
					context.claims.claim("roles", principal.roles.map { it.code })
				}
			}
		}

	/**
	 * 为 reference access token 的持久化 claims 写入 Backend 权限和角色。
	 */
	@Bean
	fun opaqueCustomizer(): OAuth2TokenCustomizer<OAuth2TokenClaimsContext> =
		OAuth2TokenCustomizer { context ->
			if (context.tokenType == OAuth2TokenType.ACCESS_TOKEN) {
				context.getPrincipal<Authentication>()?.backendPrincipal()?.let { principal ->
					context.claims.claim("access_nodes", principal.scopedAccessNodeCodes(context.authorizedScopes))
					context.claims.claim("roles", principal.roles.map { it.code })
				}
			}
		}

	/**
	 * 只在用户认证来自 Backend RBAC 时追加项目权限 claim。
	 */
	private fun org.springframework.security.core.Authentication.backendPrincipal(): SecurityUserPrincipal? =
		principal as? SecurityUserPrincipal

	private fun SecurityUserPrincipal.scopedAccessNodeCodes(authorizedScopes: Set<String>): List<String> =
		accessNodes
			.map { it.code }
			.filter { code ->
				authorizedScopes.any { scope -> code == scope || code.isMenuNodeForScope(scope) }
			}
			.sorted()

	private fun String.isMenuNodeForScope(scope: String): Boolean =
		when (scope) {
			SECURITY_ADMIN_ACCESS_NODE -> startsWith("system")
			BATTLE_RULES_ADMIN_ACCESS_NODE -> startsWith("battle-rules")
			GAME_DATA_ADMIN_ACCESS_NODE -> startsWith("game-data")
			else -> false
		}
}
