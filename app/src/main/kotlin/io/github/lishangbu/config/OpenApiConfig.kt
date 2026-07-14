package io.github.lishangbu.config

import io.github.lishangbu.common.web.openapi.OPENAPI_TOKEN_SECURITY_SCHEME
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.springdoc.core.customizers.GlobalOpenApiCustomizer
import org.springdoc.core.models.GroupedOpenApi
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/** 集中配置应用 OpenAPI 元数据、Sa-Token 安全方案和文档分组。 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfig {
	@Bean
	fun openApiAccessNodeCatalogCustomizer(sqlClient: KSqlClient): GlobalOpenApiCustomizer =
		OpenApiAccessNodeCatalogCustomizer(sqlClient)

	@Bean
	fun openApiIdentifierSchemaCustomizer(): GlobalOpenApiCustomizer = OpenApiIdentifierSchemaCustomizer()

	@Bean
	fun openApiResponseRequiredPropertiesCustomizer(): GlobalOpenApiCustomizer =
		OpenApiResponseRequiredPropertiesCustomizer()

	@Bean
	fun avalonOpenApi(): OpenAPI =
		OpenAPI()
			.info(
				Info()
					.title("Avalon Backend API")
					.version("0.0.1")
					.description(
						"后台和玩家 API 统一使用 Sa-Token 登录。调用 /api/auth/login 后，" +
							"通过 avalon-token 请求头访问受保护接口。",
					)
					.contact(Contact().name("Avalon Backend").url("https://github.com/lishangbu/avalon"))
					.license(License().name("Project License")),
			)
			.addServersItem(Server().url("http://localhost:8080").description("本地开发环境"))
			.components(
				Components().addSecuritySchemes(
					OPENAPI_TOKEN_SECURITY_SCHEME,
					SecurityScheme()
						.type(SecurityScheme.Type.APIKEY)
						.name("avalon-token")
						.`in`(SecurityScheme.In.HEADER)
						.description("填写登录接口返回的 tokenValue。"),
				),
			)

	@Bean
	fun systemOpenApiGroup(): GroupedOpenApi =
		GroupedOpenApi.builder()
			.group("system")
			.displayName("系统管理 API")
			.pathsToMatch("/api/auth/**", "/api/session", "/api/system/**")
			.build()

	@Bean
	fun adminOpenApiGroup(): GroupedOpenApi =
		GroupedOpenApi.builder()
			.group("admin")
			.displayName("后台 API")
			.pathsToMatch(
				"/api/auth/**",
				"/api/session",
				"/api/system/**",
				"/api/battle-rules/**",
				"/api/battle-sandbox/**",
				"/api/battle-sessions/**",
				"/api/game-data/**",
			)
			.build()

	@Bean
	fun playerOpenApiGroup(): GroupedOpenApi =
		GroupedOpenApi.builder()
			.group("player")
			.displayName("玩家 API")
			.pathsToMatch("/api/auth/**", "/api/player/**")
			.build()

	@Bean
	fun battleRulesOpenApiGroup(): GroupedOpenApi =
		GroupedOpenApi.builder()
			.group("battle-rules")
			.displayName("战斗规则 API")
			.pathsToMatch("/api/battle-rules/**")
			.build()

	@Bean
	fun battleSandboxOpenApiGroup(): GroupedOpenApi =
		GroupedOpenApi.builder()
			.group("battle-sandbox")
			.displayName("战斗沙箱 API")
			.pathsToMatch("/api/battle-sandbox/**")
			.build()
}
