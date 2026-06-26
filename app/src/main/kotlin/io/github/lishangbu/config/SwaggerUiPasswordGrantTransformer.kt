package io.github.lishangbu.config

import io.github.lishangbu.security.oauth.PASSWORD_GRANT_TYPE_VALUE
import jakarta.servlet.http.HttpServletRequest
import org.springdoc.core.properties.SwaggerUiConfigProperties
import org.springdoc.core.properties.SwaggerUiOAuthProperties
import org.springdoc.core.providers.ObjectMapperProvider
import org.springdoc.core.utils.Constants.SWAGGER_INITIALIZER_JS
import org.springdoc.webmvc.ui.SwaggerIndexPageTransformer
import org.springdoc.webmvc.ui.SwaggerWelcomeCommon
import org.springframework.core.io.Resource
import org.springframework.web.servlet.resource.ResourceTransformerChain
import org.springframework.web.servlet.resource.TransformedResource
import java.nio.charset.StandardCharsets

private const val SWAGGER_UI_PRESETS_MARKER = "presets: ["
private const val SWAGGER_UI_REQUEST_INTERCEPTOR_MARKER = "requestInterceptor: (request) => {\n"

/**
 * 在 springdoc 生成的 Swagger UI 初始化脚本中注入 Backend 自定义 password grant 适配逻辑。
 *
 * Swagger UI 对 OpenAPI `password` flow 会固定提交 `grant_type=password`。Backend 的授权服务器
 * 使用自定义 grant type URN 区分自身的密码授权扩展，因此这里只在请求 `/oauth2/token` 时把表单里的
 * `grant_type` 改写为授权服务器实际识别的值，避免放宽后端 token endpoint 的协议边界。
 */
class SwaggerUiPasswordGrantTransformer(
	swaggerUiConfig: SwaggerUiConfigProperties,
	swaggerUiOAuthProperties: SwaggerUiOAuthProperties,
	swaggerWelcomeCommon: SwaggerWelcomeCommon,
	objectMapperProvider: ObjectMapperProvider,
) : SwaggerIndexPageTransformer(
	swaggerUiConfig,
	swaggerUiOAuthProperties,
	swaggerWelcomeCommon,
	objectMapperProvider,
) {
	override fun transform(
		request: HttpServletRequest,
		resource: Resource,
		transformerChain: ResourceTransformerChain,
	): Resource {
		val transformedResource = super.transform(request, resource, transformerChain)
		if (!resource.isSwaggerInitializer()) {
			return transformedResource
		}

		val script = transformedResource.inputStream.use { inputStream ->
			inputStream.readBytes().toString(StandardCharsets.UTF_8)
		}
		val customizedScript = script.withPasswordGrantRequestInterceptor()
		return TransformedResource(
			transformedResource,
			customizedScript.toByteArray(StandardCharsets.UTF_8),
		)
	}

	private fun Resource.isSwaggerInitializer(): Boolean =
		filename == SWAGGER_INITIALIZER_JS ||
			runCatching { url.toString().contains(SWAGGER_INITIALIZER_JS) }.getOrDefault(false)

	private fun String.withPasswordGrantRequestInterceptor(): String {
		if (contains(PASSWORD_GRANT_TYPE_VALUE)) {
			return this
		}

		if (contains(SWAGGER_UI_REQUEST_INTERCEPTOR_MARKER)) {
			return replace(
				SWAGGER_UI_REQUEST_INTERCEPTOR_MARKER,
				SWAGGER_UI_REQUEST_INTERCEPTOR_MARKER + passwordGrantRewriteScript(),
			)
		}

		return replace(
			SWAGGER_UI_PRESETS_MARKER,
			"""
			requestInterceptor: (request) => {
			${passwordGrantRewriteScript()}			return request;
					},
					$SWAGGER_UI_PRESETS_MARKER
			""".trimIndent(),
		)
	}

	private fun passwordGrantRewriteScript(): String =
		"""
					if (request.url && request.body) {
						const tokenUrl = new URL(request.url, window.location.origin);
						const customGrantType = "$PASSWORD_GRANT_TYPE_VALUE";
						if (tokenUrl.pathname.endsWith("$OPENAPI_OAUTH_TOKEN_URL")) {
							if (typeof request.body === "string") {
								const formBody = new URLSearchParams(request.body);
								if (formBody.get("grant_type") === "password") {
									formBody.set("grant_type", customGrantType);
									request.body = formBody.toString();
								}
							} else if (request.body instanceof URLSearchParams && request.body.get("grant_type") === "password") {
								request.body.set("grant_type", customGrantType);
							}
						}
					}
		""".trimIndent().prependIndent("\t\t\t") + "\n"
}
