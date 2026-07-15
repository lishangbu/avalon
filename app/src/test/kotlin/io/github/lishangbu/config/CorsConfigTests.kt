package io.github.lishangbu.config

import io.github.lishangbu.match.game.MatchStartupRecovery
import io.github.lishangbu.scheduler.ScheduledTaskManagementService
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

/**
 * 验证应用边界的 CORS 配置允许管理端本地开发来源预检安全 API。
 */
@Tag("integration")
@SpringBootTest(
	webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
	properties = [
		"spring.autoconfigure.exclude=org.springframework.boot.quartz.autoconfigure.QuartzAutoConfiguration,io.github.lishangbu.scheduler.SchedulerAutoConfiguration",
		"backend.cors.allowed-origins[0]=http://localhost:5173",
		"backend.cors.allowed-origins[1]=http://127.0.0.1:5174",
		"spring.liquibase.enabled=false",
	],
)
class CorsConfigTests(
	@LocalServerPort private val port: Int,
) {
	/** CORS 预检不需要启动调度管理和对战恢复任务。 */
	@MockitoBean
	private lateinit var scheduledTaskManagementService: ScheduledTaskManagementService

	@MockitoBean
	private lateinit var matchStartupRecovery: MatchStartupRecovery

	@Test
	fun `admin ui origin can preflight security api path`() {
		assertSecurityApiPreflightAllowed("http://localhost:5173")
	}

	@Test
	fun `fallback local admin ui origin can preflight security api path`() {
		assertSecurityApiPreflightAllowed("http://127.0.0.1:5174")
	}

	private fun assertSecurityApiPreflightAllowed(origin: String) {
		val request = HttpRequest.newBuilder()
			.uri(URI.create("http://localhost:$port/api/system/rbac/access-nodes"))
			.method("OPTIONS", HttpRequest.BodyPublishers.noBody())
			.header(HttpHeaders.ORIGIN, origin)
			.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
			.build()

		val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.discarding())

		assertThat(response.statusCode()).isEqualTo(200)
		assertThat(response.headers().firstValue(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
			.hasValue(origin)
	}
}
