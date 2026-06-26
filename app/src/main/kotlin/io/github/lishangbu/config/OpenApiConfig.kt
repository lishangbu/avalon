package io.github.lishangbu.config

import io.github.lishangbu.security.oauth.PASSWORD_GRANT_TYPE_VALUE
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
import org.springdoc.core.providers.ObjectMapperProvider
import org.springdoc.core.models.GroupedOpenApi
import org.springdoc.webmvc.ui.SwaggerIndexTransformer
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Controller `@SecurityRequirement` 统一引用的 OpenAPI 安全方案名称。
 *
 * 名称继续保留 `bearerAuth`，避免既有接口注解和前端生成规则跟随认证方式展示细节一起变动。
 */
const val OPENAPI_BEARER_SECURITY_SCHEME = "bearerAuth"

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
	fun avalonOpenApi(): OpenAPI =
		OpenAPI()
			.info(
				Info()
					.title("Avalon Backend API")
					.version("0.0.1")
					.description(
						"""
						Avalon 后端管理 API 文档。文档覆盖当前登录态、RBAC、OAuth client、JWK 与定时任务管理接口。

						除 OAuth2 标准授权端点外，`/api/system/**` 管理接口需要携带 Bearer access token，并要求令牌具备 `security:admin` 权限。
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
													.addString(SECURITY_ADMIN_ACCESS_NODE, "系统管理 API 访问权限"),
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
