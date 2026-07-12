package io.github.lishangbu.config

import io.github.lishangbu.common.web.openapi.OPENAPI_BEARER_SECURITY_SCHEME
import io.github.lishangbu.security.oauth.PASSWORD_GRANT_TYPE_VALUE
import io.github.lishangbu.security.rbac.BATTLE_RULES_ADMIN_ACCESS_NODE
import io.github.lishangbu.security.rbac.BATTLE_SANDBOX_RUN_ACCESS_NODE
import io.github.lishangbu.security.rbac.BATTLE_SESSIONS_RUN_ACCESS_NODE
import io.github.lishangbu.security.rbac.GAME_DATA_ADMIN_ACCESS_NODE
import io.github.lishangbu.security.rbac.SECURITY_ADMIN_ACCESS_NODE
import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.OAuthFlow
import io.swagger.v3.oas.models.security.OAuthFlows
import io.swagger.v3.oas.models.security.Scopes
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springdoc.core.properties.SwaggerUiConfigProperties
import org.springdoc.core.properties.SwaggerUiOAuthProperties
import org.springdoc.core.customizers.GlobalOpenApiCustomizer
import org.springdoc.core.providers.ObjectMapperProvider
import org.springdoc.core.models.GroupedOpenApi
import org.springdoc.webmvc.ui.SwaggerIndexTransformer
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Swagger UI password flow 获取 access token 时调用的授权服务器 token endpoint。
 */
const val OPENAPI_OAUTH_TOKEN_URL = "/oauth2/token"

/**
 * 应用级 OpenAPI 文档装配。
 *
 * 这里集中维护文档标题、服务地址、安全方案和 API 分组；具体业务语义仍由各 Controller
 * 与 DTO 的 OpenAPI 注解承载，确保接口文档随代码边界同步演进。
 */
@Configuration(proxyBeanMethods = false)
class OpenApiConfig {
	@Bean
	fun openApiIdentifierSchemaCustomizer(): GlobalOpenApiCustomizer =
		OpenApiIdentifierSchemaCustomizer()

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
						"""
						Avalon 后端管理 API 文档。文档覆盖当前登录态、RBAC、OAuth client、OAuth token、JWK、定时任务、战斗规则、战斗沙盒、战斗会话和游戏资料管理接口。

						除 OAuth2 标准授权端点外，管理接口需要携带 Bearer access token。`/api/system/**` 需要 `security:admin` 权限，`/api/battle-rules/**` 需要 `battle-rules:admin` 权限，`/api/battle-sandbox/**` 需要 `battle-sandbox:run` 权限，`/api/battle-sessions/**` 需要 `battle-sessions:run` 权限，`/api/game-data/**` 需要 `game-data:admin` 权限。
						分页接口统一使用从 0 开始的 `page` 和最大 100 的 `size` 参数；错误响应统一返回稳定的 `code`、`message` 与可选 `field`。
						""".trimIndent(),
					)
					.contact(
						Contact()
							.name("Avalon Backend")
							.url("https://github.com/lishangbu/avalon"),
					)
					.license(
						License()
							.name("Project License"),
					),
			)
			.addServersItem(
				Server()
					.url("http://localhost:8080")
					.description("本地开发环境"),
			)
			.components(
				Components()
					.addSecuritySchemes(
						OPENAPI_BEARER_SECURITY_SCHEME,
						SecurityScheme()
							.type(SecurityScheme.Type.OAUTH2)
							.description(
								"Swagger UI 通过 Backend 自定义 password grant 获取 access token；" +
									"token endpoint 接收的 grant_type 为 `$PASSWORD_GRANT_TYPE_VALUE`，" +
									"成功后业务接口仍使用 Bearer access token 访问。",
							)
							.flows(
								OAuthFlows()
									.password(
										OAuthFlow()
											.tokenUrl(OPENAPI_OAUTH_TOKEN_URL)
											.scopes(
												Scopes()
													.addString(SECURITY_ADMIN_ACCESS_NODE, "系统管理 API 访问权限")
													.addString(BATTLE_RULES_ADMIN_ACCESS_NODE, "战斗规则管理 API 访问权限")
													.addString(BATTLE_SANDBOX_RUN_ACCESS_NODE, "战斗沙盒执行 API 访问权限")
													.addString(BATTLE_SESSIONS_RUN_ACCESS_NODE, "战斗会话执行 API 访问权限")
													.addString(GAME_DATA_ADMIN_ACCESS_NODE, "游戏资料管理 API 访问权限"),
											),
									),
							),
					),
			)

	@Bean
	fun systemOpenApiGroup(): GroupedOpenApi =
		GroupedOpenApi.builder()
			.group("system")
			.displayName("系统管理 API")
			.pathsToMatch("/api/session", "/api/system/**")
			.build()

	@Bean
	fun adminOpenApiGroup(): GroupedOpenApi =
		GroupedOpenApi.builder()
			.group("admin")
			.displayName("后台 API")
			.pathsToMatch(
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
			.pathsToMatch("/api/player/**")
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
			.displayName("战斗沙盒 API")
			.pathsToMatch("/api/battle-sandbox/**")
			.build()

	/**
	 * 替换 springdoc 默认 Swagger UI 初始化脚本 transformer，补齐自定义 password grant 的请求改写。
	 */
	@Bean
	fun swaggerUiPasswordGrantTransformer(
		swaggerUiConfig: SwaggerUiConfigProperties,
		swaggerUiOAuthProperties: SwaggerUiOAuthProperties,
		swaggerWelcomeCommon: SwaggerWelcomeCommon,
		objectMapperProvider: ObjectMapperProvider,
	): SwaggerIndexTransformer =
		SwaggerUiPasswordGrantTransformer(
			swaggerUiConfig = swaggerUiConfig,
			swaggerUiOAuthProperties = swaggerUiOAuthProperties,
			swaggerWelcomeCommon = swaggerWelcomeCommon,
			objectMapperProvider = objectMapperProvider,
		)
}
