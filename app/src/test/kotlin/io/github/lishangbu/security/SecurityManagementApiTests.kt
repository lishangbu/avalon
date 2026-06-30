package io.github.lishangbu.security

import com.jayway.jsonpath.JsonPath
import io.github.lishangbu.BackendApplication
import io.github.lishangbu.security.entity.OAuth2Jwk
import io.github.lishangbu.security.entity.SecurityUser
import io.github.lishangbu.security.entity.active
import io.github.lishangbu.security.entity.keyId
import io.github.lishangbu.security.entity.roles
import io.github.lishangbu.security.repository.SecurityUserRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.assertj.core.api.Assertions.assertThat
import org.hamcrest.Matchers.hasItem
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

@SpringBootTest(
	classes = [BackendApplication::class],
	properties = [
		"spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.yaml",
	],
)
@ContextConfiguration(initializers = [SecurityManagementApiPostgresTestContainer::class])
/**
 * 绔埌绔獙璇佸畨鍏ㄧ鐞?API 鐨勯壌鏉冦€丱Auth client 绠＄悊銆丣WK 杞崲鍜?RBAC 鍐欏叆銆? */
class SecurityManagementApiTests(
	@Autowired private val userRepository: SecurityUserRepository,
	@Autowired private val sqlClient: KSqlClient,
	@Autowired private val webApplicationContext: WebApplicationContext,
) {
	private lateinit var mockMvc: MockMvc

	@BeforeEach
	fun setUpMockMvc() {
		mockMvc = MockMvcBuilders
			.webAppContextSetup(webApplicationContext)
			.apply<org.springframework.test.web.servlet.setup.DefaultMockMvcBuilder>(springSecurity())
			.build()
	}

	@Test
	fun `security management api requires authentication`() {
		mockMvc.perform(get("/api/system/oauth/clients"))
			.andExpect(status().isUnauthorized)

		mockMvc.perform(get("/api/session"))
			.andExpect(status().isUnauthorized)
	}

	@Test
	fun `authenticated user can load session menus for antd layout`() {
		insertUser("menu-manager", 201)
		val token = issueToken("menu-manager", "security:admin")

		val response = mockMvc.perform(
			get("/api/session")
				.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.user.username").value("menu-manager"))
			.andExpect(jsonPath("$.user.displayName").value("menu-manager"))
			.andExpect(jsonPath("$.roles[*].code", hasItem("system-admin")))
			.andExpect(jsonPath("$.accessNodeCodes", hasItem("security:admin")))
			.andExpect(jsonPath("$.menus[0].code").value("system"))
			.andExpect(jsonPath("$.menus[0].icon").value("lucide:settings"))
			.andExpect(jsonPath("$.menus[0].children[0].code").value("system.rbac"))
			.andExpect(jsonPath("$.menus[0].children[0].icon").value("lucide:shield-check"))
			.andExpect(jsonPath("$.menus[0].children[0].children[0].code").value("system.rbac.users"))
			.andExpect(jsonPath("$.menus[0].children[0].children[0].componentKey").value("system/rbac/users"))
			.andExpect(jsonPath("$.menus[0].children[0].children[0].icon").value("lucide:users"))
			.andReturn()
			.response
			.contentAsString

		val menuCodes = JsonPath.read<List<String>>(response, "$.menus..code")
		assertThat(menuCodes)
			.contains("system.rbac.access-nodes", "system.oauth.clients", "system.oauth.tokens", "system.scheduler.tasks")
			.doesNotContain("security:admin")

		insertUser("battle-menu-manager", 203)
		val battleToken = issueToken("battle-menu-manager", "battle-rules:admin")
		val battleResponse = mockMvc.perform(
			get("/api/session")
				.header("Authorization", "Bearer $battleToken"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.roles[*].code", hasItem("battle-rules-admin")))
			.andExpect(jsonPath("$.accessNodeCodes", hasItem("battle-rules:admin")))
			.andReturn()
			.response
			.contentAsString

		val battleMenuCodes = JsonPath.read<List<String>>(battleResponse, "$.menus..code")
		assertThat(battleMenuCodes)
			.contains("battle-rules.coverage", "battle-rules.fixtures", "battle-rules.test-runs")
			.doesNotContain("battle-rules.fixture-sources")
	}

	@Test
	fun `security admin can manage scheduled tasks and inspect executions`() {
		insertUser("scheduler-manager", 201)
		val token = issueToken("scheduler-manager", "security:admin")
		ManagementApiScheduledTaskHandler.latch = CountDownLatch(1)
		ManagementApiScheduledTaskHandler.executions.clear()

		val createResponse = mockMvc.perform(
			post("/api/system/scheduler/tasks")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "code": "cleanup-expired-token",
					  "handlerCode": "test.echo",
					  "name": "Cleanup Expired Token",
					  "description": "Remove expired authorization state",
					  "groupName": "system",
					  "scheduleType": "CRON",
					  "cronExpression": "0 0/5 * * * ?",
					  "timeZone": "UTC",
					  "payload": { "scope": "expired-token" },
					  "enabled": true
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.code").value("cleanup-expired-token"))
			.andExpect(jsonPath("$.handlerCode").value("test.echo"))
			.andExpect(jsonPath("$.enabled").value(true))
			.andReturn()
			.response
			.contentAsString
		val taskId: Long = JsonPath.read(createResponse, "$.id")

		mockMvc.perform(
			post("/api/system/scheduler/tasks/$taskId/trigger")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""{"payload":{"scope":"manual"}}"""),
		)
			.andExpect(status().isAccepted)
			.andExpect(jsonPath("$.triggered").value(true))

		assertThat(ManagementApiScheduledTaskHandler.latch.await(3, TimeUnit.SECONDS)).isTrue()

		mockMvc.perform(
			get("/api/system/scheduler/tasks")
				.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.rows[?(@.code == 'cleanup-expired-token')].lastExecutionStatus").value(hasItem("SUCCESS")))

		mockMvc.perform(
			get("/api/system/scheduler/tasks/$taskId/executions")
				.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.rows[0].status").value("SUCCESS"))
			.andExpect(jsonPath("$.rows[0].payloadSnapshot.scope").value("manual"))

		mockMvc.perform(
			post("/api/system/scheduler/tasks/$taskId/disable")
				.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.enabled").value(false))
	}

	@Test
	fun `security admin can manage oauth clients and jwks`() {
		insertUser("oauth-manager", 201)
		val token = issueToken("oauth-manager", "security:admin")

		mockMvc.perform(
			get("/api/system/oauth/clients")
				.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.rows[*].clientId", hasItem("system-admin-jwt")))

		val createdClientResponse = mockMvc.perform(
			post("/api/system/oauth/clients")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "clientId": "system-tools-jwt",
					  "clientSecret": "{noop}tools-secret",
					  "clientName": "Tools JWT Client",
					  "scopes": ["security:admin"],
					  "accessTokenFormat": "self-contained"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.clientId").value("system-tools-jwt"))
			.andExpect(jsonPath("$.accessTokenFormat").value("self-contained"))
			.andReturn()
			.response
			.contentAsString
		val toolsClientId = JsonPath.read<String>(createdClientResponse, "$.clientId")

		mockMvc.perform(
			get("/api/system/oauth/clients/{clientId}", toolsClientId)
				.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.clientId").value("system-tools-jwt"))
			.andExpect(jsonPath("$.clientName").value("Tools JWT Client"))

		mockMvc.perform(
			put("/api/system/oauth/clients/{clientId}", toolsClientId)
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "clientName": "Tools Reference Client",
					  "scopes": ["security:admin"],
					  "accessTokenFormat": "reference",
					  "accessTokenTtlSeconds": 900,
					  "refreshTokenTtlSeconds": 3600
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.clientName").value("Tools Reference Client"))
			.andExpect(jsonPath("$.accessTokenFormat").value("reference"))
			.andExpect(jsonPath("$.accessTokenTtlSeconds").value(900))
			.andExpect(jsonPath("$.refreshTokenTtlSeconds").value(3600))

		mockMvc.perform(
			put("/api/system/oauth/clients/{clientId}/secret", toolsClientId)
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "clientSecret": "{noop}tools-secret-v2"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.clientId").value("system-tools-jwt"))

		assertThat(
			issueToken(
				username = "oauth-manager",
				scope = "security:admin",
				clientId = toolsClientId,
				clientSecret = "tools-secret-v2",
			),
		).isNotBlank()

		val firstKeyId = mockMvc.perform(
			get("/api/system/oauth/jwks")
				.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.rows[0].active").value(true))
			.andReturn()
			.response
			.contentAsString
			.let { JsonPath.read<String>(it, "$.rows[0].keyId") }

		mockMvc.perform(
			get("/api/system/oauth/jwks/{keyId}", firstKeyId)
				.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.keyId").value(firstKeyId))
			.andExpect(jsonPath("$.active").value(true))

		mockMvc.perform(
			post("/api/system/oauth/jwks/rotation")
				.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.active").value(true))

		val activeKeys = sqlClient.executeQuery(OAuth2Jwk::class) {
			where(table.active eq true)
			orderBy(table.keyId)
			select(table.keyId)
		}
		assertThat(activeKeys).hasSize(1)
		assertThat(activeKeys.single()).isNotEqualTo(firstKeyId)
	}

	@Test
	fun `security admin can inspect and revoke opaque oauth tokens`() {
		insertUser("token-manager", 201)
		insertUser("token-target", 201)
		val adminToken = issueToken(
			username = "token-manager",
			scope = "security:admin",
			clientId = "system-admin-opaque",
			clientSecret = "system-admin-opaque-secret",
		)
		val targetToken = issueToken(
			username = "token-target",
			scope = "security:admin",
			clientId = "system-admin-opaque",
			clientSecret = "system-admin-opaque-secret",
		)

		val listResponse = mockMvc.perform(
			get("/api/system/oauth/tokens")
				.header("Authorization", "Bearer $adminToken")
				.param("clientId", "system-admin-opaque")
				.param("principalName", "token-target"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.rows[0].principalName").value("token-target"))
			.andExpect(jsonPath("$.rows[0].clientId").value("system-admin-opaque"))
			.andExpect(jsonPath("$.rows[0].status").value("ACTIVE"))
			.andExpect(jsonPath("$.rows[0].active").value(true))
			.andReturn()
			.response
			.contentAsString
		val authorizationId = JsonPath.read<String>(listResponse, "$.rows[0].id")

		mockMvc.perform(
			get("/api/system/oauth/tokens/{authorizationId}", authorizationId)
				.header("Authorization", "Bearer $adminToken"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.id").value(authorizationId))
			.andExpect(jsonPath("$.accessTokenScopes", hasItem("security:admin")))

		mockMvc.perform(
			post("/api/system/oauth/tokens/{authorizationId}/revoke", authorizationId)
				.header("Authorization", "Bearer $adminToken"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.id").value(authorizationId))
			.andExpect(jsonPath("$.status").value("REVOKED"))
			.andExpect(jsonPath("$.active").value(false))

		mockMvc.perform(
			get("/api/session")
				.header("Authorization", "Bearer $targetToken"),
		)
			.andExpect(status().isUnauthorized)
	}

	@Test
	fun `security admin can manage roles and users`() {
		insertUser("rbac-manager", 201)
		val token = issueToken("rbac-manager", "security:admin")

		mockMvc.perform(
			get("/api/system/rbac/access-nodes")
				.header("Authorization", "Bearer $token")
				.param("codePrefix", "security"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.rows[*].code", hasItem("security:admin")))

		mockMvc.perform(
			get("/api/system/rbac/access-nodes/{accessNodeCode}", "security:admin")
				.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.code").value("security:admin"))
			.andExpect(jsonPath("$.type").value("API"))
			.andExpect(jsonPath("$.visible").value(false))
			.andExpect(jsonPath("$.enabled").value(true))
			.andExpect(jsonPath("$.apiPattern").value("/api/system/**"))

		mockMvc.perform(
			get("/api/system/rbac/access-nodes/{accessNodeCode}", "system.rbac.users")
				.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.code").value("system.rbac.users"))
			.andExpect(jsonPath("$.type").value("ROUTE"))
			.andExpect(jsonPath("$.path").value("/system/rbac/users"))
			.andExpect(jsonPath("$.componentKey").value("system/rbac/users"))
			.andExpect(jsonPath("$.visible").value(true))

		mockMvc.perform(
			get("/api/system/rbac/access-nodes")
				.header("Authorization", "Bearer $token")
				.param("codePrefix", "missing"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.totalRowCount").value(0))
			.andExpect(jsonPath("$.rows.length()").value(0))

		mockMvc.perform(
			get("/api/system/rbac/access-nodes")
				.header("Authorization", "Bearer $token")
				.param("codePrefix", "security")
				.param("page", "0")
				.param("size", "1"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.totalRowCount").value(1))
			.andExpect(jsonPath("$.rows[0].code").value("security:admin"))

		mockMvc.perform(
			get("/api/system/rbac/access-nodes")
				.header("Authorization", "Bearer $token")
				.param("type", "ROUTE")
				.param("visible", "true")
				.param("q", "rbac"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.rows[*].code", hasItem("system.rbac.users")))
			.andExpect(jsonPath("$.rows[*].type", hasItem("ROUTE")))

		val createdRoleResponse = mockMvc.perform(
			post("/api/system/rbac/roles")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "code": "audit-admin",
					  "name": "Audit Admin",
					  "accessNodeCodes": ["security:admin"]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.code").value("audit-admin"))
			.andExpect(jsonPath("$.accessNodeCodes", hasItem("security:admin")))
			.andReturn()
			.response
			.contentAsString
		val auditRoleId = JsonPath.read<Number>(createdRoleResponse, "$.id").toLong()

		mockMvc.perform(
			get("/api/system/rbac/roles")
				.header("Authorization", "Bearer $token")
				.param("accessNodeCode", "missing:access"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.totalRowCount").value(0))
			.andExpect(jsonPath("$.rows.length()").value(0))

		mockMvc.perform(
			get("/api/system/rbac/roles")
				.header("Authorization", "Bearer $token")
				.param("accessNodeCode", "security:admin")
				.param("q", "audit")
				.param("page", "0")
				.param("size", "1"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.totalRowCount").value(1))
			.andExpect(jsonPath("$.rows[0].code").value("audit-admin"))
			.andExpect(jsonPath("$.rows[0].accessNodeCodes", hasItem("security:admin")))

		mockMvc.perform(
			get("/api/system/rbac/roles/{roleId}", auditRoleId)
				.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.code").value("audit-admin"))
			.andExpect(jsonPath("$.accessNodeCodes", hasItem("security:admin")))

		mockMvc.perform(
			put("/api/system/rbac/roles/{roleId}", auditRoleId)
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "name": "Audit Owner",
					  "accessNodeCodes": ["security:admin"]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.code").value("audit-admin"))
			.andExpect(jsonPath("$.name").value("Audit Owner"))
			.andExpect(jsonPath("$.accessNodeCodes[0]").value("security:admin"))

		val createdUserResponse = mockMvc.perform(
			post("/api/system/rbac/users")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "username": "auditor",
					  "password": "secret123",
					  "displayName": "Auditor",
					  "roleCodes": ["audit-admin"]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isCreated)
			.andExpect(jsonPath("$.username").value("auditor"))
			.andExpect(jsonPath("$.roleCodes", hasItem("audit-admin")))
			.andReturn()
			.response
			.contentAsString
		val auditorUserId = JsonPath.read<Number>(createdUserResponse, "$.id").toLong()

		mockMvc.perform(
			get("/api/system/rbac/users")
				.header("Authorization", "Bearer $token")
				.param("roleCode", "missing-role"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.totalRowCount").value(0))
			.andExpect(jsonPath("$.rows.length()").value(0))

		mockMvc.perform(
			get("/api/system/rbac/users")
				.header("Authorization", "Bearer $token")
				.param("roleCode", "audit-admin")
				.param("page", "0")
				.param("size", "1"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.totalRowCount").value(1))
			.andExpect(jsonPath("$.rows[0].username").value("auditor"))
			.andExpect(jsonPath("$.rows[0].roleCodes", hasItem("audit-admin")))

		mockMvc.perform(
			get("/api/system/rbac/users")
				.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.rows[*].username", hasItem("auditor")))

		mockMvc.perform(
			get("/api/system/rbac/users")
				.header("Authorization", "Bearer $token")
				.param("q", "audit")
				.param("page", "0")
				.param("size", "1"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.totalRowCount").value(1))
			.andExpect(jsonPath("$.rows[0].username").value("auditor"))

		mockMvc.perform(
			get("/api/system/rbac/users/{userId}", auditorUserId)
				.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.username").value("auditor"))
			.andExpect(jsonPath("$.roleCodes", hasItem("audit-admin")))

		mockMvc.perform(
			put("/api/system/rbac/users/{userId}/roles", auditorUserId)
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "roleCodes": ["system-admin"]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.roleCodes[0]").value("system-admin"))
			.andExpect(jsonPath("$.roleCodes.length()").value(1))

		mockMvc.perform(
			put("/api/system/rbac/users/{userId}/password", auditorUserId)
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "password": "secret456"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.username").value("auditor"))

		mockMvc.perform(
			post("/api/system/rbac/users/{userId}/disable", auditorUserId)
			.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.enabled").value(false))

		mockMvc.perform(
			get("/api/system/rbac/users")
				.header("Authorization", "Bearer $token")
				.param("enabled", "false"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.totalRowCount").value(1))
			.andExpect(jsonPath("$.rows[0].username").value("auditor"))
			.andExpect(jsonPath("$.rows[0].enabled").value(false))

		mockMvc.perform(
			post("/api/system/rbac/users/{userId}/enable", auditorUserId)
				.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.enabled").value(true))

		mockMvc.perform(
			post("/api/system/rbac/users/{userId}/lock", auditorUserId)
			.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.accountNonLocked").value(false))

		mockMvc.perform(
			get("/api/system/rbac/users")
				.header("Authorization", "Bearer $token")
				.param("accountNonLocked", "false"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.totalRowCount").value(1))
			.andExpect(jsonPath("$.rows[0].username").value("auditor"))
			.andExpect(jsonPath("$.rows[0].accountNonLocked").value(false))

		mockMvc.perform(
			post("/api/system/rbac/users/{userId}/unlock", auditorUserId)
				.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isOk)
			.andExpect(jsonPath("$.accountNonLocked").value(true))

		assertThat(issueToken("auditor", "security:admin", "secret456")).isNotBlank()
	}

	@Test
	fun `system api returns stable validation errors`() {
		insertUser("error-manager", 201)
		val token = issueToken("error-manager", "security:admin")

		mockMvc.perform(
			post("/api/system/oauth/clients")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "clientId": "",
					  "clientSecret": "{noop}tools-secret",
					  "clientName": "Tools JWT Client",
					  "scopes": ["security:admin"],
					  "accessTokenFormat": "self-contained"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("validation.required"))
			.andExpect(jsonPath("$.field").value("clientId"))
			.andExpect(jsonPath("$.message").value("clientId 不能为空"))

		mockMvc.perform(
			post("/api/system/oauth/clients")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "clientId": "system-extra-client",
					  "clientSecret": "{noop}tools-secret",
					  "clientName": "Tools JWT Client",
					  "scopes": ["system:read"],
					  "accessTokenFormat": "self-contained"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("validation.unsupported"))
			.andExpect(jsonPath("$.field").value("scopes"))

		mockMvc.perform(
			post("/api/system/oauth/clients")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "clientId": "short-secret-client",
					  "clientSecret": "short",
					  "clientName": "Tools JWT Client",
					  "scopes": ["security:admin"],
					  "accessTokenFormat": "self-contained"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("validation.invalid"))
			.andExpect(jsonPath("$.field").value("clientSecret"))

		mockMvc.perform(
			post("/api/system/oauth/clients")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "clientId": "blank-scope-client",
					  "clientSecret": "{noop}tools-secret",
					  "clientName": "Tools JWT Client",
					  "scopes": ["security:admin", "  "],
					  "accessTokenFormat": "self-contained"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("validation.invalid"))
			.andExpect(jsonPath("$.field").value("scopes"))

		mockMvc.perform(
			post("/api/system/oauth/clients")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "clientId": "duplicate-scope-client",
					  "clientSecret": "{noop}tools-secret",
					  "clientName": "Tools JWT Client",
					  "scopes": ["security:admin", "security:admin"],
					  "accessTokenFormat": "self-contained"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("validation.invalid"))
			.andExpect(jsonPath("$.field").value("scopes"))

		mockMvc.perform(
			put("/api/system/oauth/clients/{clientId}", "system-admin-jwt")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "clientName": "System Admin JWT Client",
					  "scopes": ["security:admin"],
					  "accessTokenFormat": "jwt",
					  "accessTokenTtlSeconds": 900,
					  "refreshTokenTtlSeconds": 3600
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("validation.unsupported"))
			.andExpect(jsonPath("$.field").value("accessTokenFormat"))

		mockMvc.perform(
			put("/api/system/oauth/clients/{clientId}", "system-admin-jwt")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "clientName": "System Admin JWT Client",
					  "scopes": ["security:admin"],
					  "accessTokenFormat": "self-contained",
					  "accessTokenTtlSeconds": 30,
					  "refreshTokenTtlSeconds": 3600
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("validation.invalid"))
			.andExpect(jsonPath("$.field").value("accessTokenTtlSeconds"))

		mockMvc.perform(
			put("/api/system/oauth/clients/{clientId}", "system-admin-jwt")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "clientName": "System Admin JWT Client",
					  "scopes": ["security:admin"],
					  "accessTokenFormat": "self-contained",
					  "accessTokenTtlSeconds": 3600,
					  "refreshTokenTtlSeconds": 300
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("validation.invalid"))
			.andExpect(jsonPath("$.field").value("refreshTokenTtlSeconds"))

		mockMvc.perform(
			put("/api/system/oauth/clients/{clientId}/secret", "system-admin-jwt")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "clientSecret": "short"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("validation.invalid"))
			.andExpect(jsonPath("$.field").value("clientSecret"))

		mockMvc.perform(
			post("/api/system/rbac/roles")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "code": "AuditAdmin",
					  "name": "Audit Admin",
					  "accessNodeCodes": ["security:admin"]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("validation.invalid"))
			.andExpect(jsonPath("$.field").value("code"))

		mockMvc.perform(
			post("/api/system/rbac/users")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "username": "short-password-user",
					  "password": "short",
					  "displayName": "Auditor",
					  "roleCodes": ["system-admin"]
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("validation.invalid"))
			.andExpect(jsonPath("$.field").value("password"))

		mockMvc.perform(
			get("/api/system/rbac/access-nodes")
				.header("Authorization", "Bearer $token")
				.param("page", "-1"),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("validation.invalid"))
			.andExpect(jsonPath("$.field").value("page"))

		mockMvc.perform(
			get("/api/system/rbac/access-nodes")
				.header("Authorization", "Bearer $token")
				.param("size", "101"),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("validation.invalid"))
			.andExpect(jsonPath("$.field").value("size"))

		mockMvc.perform(
			get("/api/system/rbac/access-nodes")
				.header("Authorization", "Bearer $token")
				.param("page", "first"),
		)
			.andExpect(status().isBadRequest)
			.andExpect(jsonPath("$.code").value("validation.invalid"))
			.andExpect(jsonPath("$.field").value("page"))

		mockMvc.perform(
			get("/api/system/rbac/users/9223372036854775807")
				.header("Authorization", "Bearer $token"),
		)
			.andExpect(status().isNotFound)
			.andExpect(jsonPath("$.code").value("resource.not_found"))
			.andExpect(jsonPath("$.field").value("userId"))
	}

	@Test
	fun `system api returns stable conflict errors`() {
		insertUser("conflict-manager", 201)
		val token = issueToken("conflict-manager", "security:admin")

		mockMvc.perform(
			post("/api/system/oauth/clients")
				.header("Authorization", "Bearer $token")
				.contentType(MediaType.APPLICATION_JSON)
				.content(
					"""
					{
					  "clientId": "system-admin-jwt",
					  "clientSecret": "{noop}tools-secret",
					  "clientName": "Tools JWT Client",
					  "scopes": ["security:admin"],
					  "accessTokenFormat": "self-contained"
					}
					""".trimIndent(),
				),
		)
			.andExpect(status().isConflict)
			.andExpect(jsonPath("$.code").value("resource.conflict"))
			.andExpect(jsonPath("$.field").value("clientId"))
	}

	private fun issueToken(
		username: String,
		scope: String,
		password: String = "secret",
		clientId: String = "system-admin-jwt",
		clientSecret: String = "system-admin-jwt-secret",
	): String {
		val response = mockMvc.perform(
			post("/oauth2/token")
				.with(httpBasic(clientId, clientSecret))
				.contentType(MediaType.APPLICATION_FORM_URLENCODED)
				.param("grant_type", "urn:security:params:oauth:grant-type:password")
				.param("username", username)
				.param("password", password)
				.param("scope", scope),
		)
			.andExpect(status().isOk)
			.andReturn()
			.response
			.contentAsString

		return JsonPath.read(response, "$.access_token")
	}

	private fun insertUser(username: String, roleId: Long) {
		val userId = nextUserId.getAndIncrement()
		userRepository.save(
			SecurityUser {
				id = userId
				this.username = username
				passwordHash = "{noop}secret"
				displayName = username
				enabled = true
				accountNonLocked = true
			},
		)
		sqlClient.getAssociations(SecurityUser::roles).insertIfAbsent(userId, roleId)
	}

	private companion object {
		private val nextUserId = AtomicLong(40001)
	}
}
