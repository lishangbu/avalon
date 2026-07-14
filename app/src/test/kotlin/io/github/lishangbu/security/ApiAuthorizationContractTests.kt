package io.github.lishangbu.security

import io.github.lishangbu.BackendApplication
import io.github.lishangbu.common.web.security.BATTLE_RULES_ADMIN_AUTHORITY
import io.github.lishangbu.common.web.security.BATTLE_SANDBOX_RUN_AUTHORITY
import io.github.lishangbu.common.web.security.BATTLE_SESSIONS_RUN_AUTHORITY
import io.github.lishangbu.common.web.security.GAME_DATA_ADMIN_AUTHORITY
import io.github.lishangbu.common.web.security.RequireBattleRulesAdmin
import io.github.lishangbu.common.web.security.RequireBattleSandboxRun
import io.github.lishangbu.common.web.security.RequireBattleSessionsRun
import io.github.lishangbu.common.web.security.RequireGameDataAdmin
import io.github.lishangbu.common.web.security.RequireSecurityAdmin
import io.github.lishangbu.common.web.security.SECURITY_ADMIN_AUTHORITY
import io.github.lishangbu.security.entity.SecurityAccessNode
import io.github.lishangbu.security.entity.code
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.annotation.AnnotatedElementUtils
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import tools.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import kotlin.reflect.KClass

/** 防止 Controller 权限注解、权限目录与 URL 领域边界独立演进时发生漂移。 */
@SpringBootTest(
	classes = [BackendApplication::class],
	properties = ["spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.yaml"],
)
@ContextConfiguration(initializers = [SecurityApiAccessPostgresTestContainer::class])
class ApiAuthorizationContractTests(
	@Autowired @Qualifier("requestMappingHandlerMapping") private val handlerMapping: RequestMappingHandlerMapping,
	@Autowired private val sqlClient: KSqlClient,
	@Autowired private val objectMapper: ObjectMapper,
) {
	@Test
	fun `protected api domains declare the expected permission annotation`() {
		val ruleByPrefix = linkedMapOf(
			"/api/system/" to PermissionRule(RequireSecurityAdmin::class, SECURITY_ADMIN_AUTHORITY),
			"/api/battle-rules/" to PermissionRule(RequireBattleRulesAdmin::class, BATTLE_RULES_ADMIN_AUTHORITY),
			"/api/battle-sandbox" to PermissionRule(RequireBattleSandboxRun::class, BATTLE_SANDBOX_RUN_AUTHORITY),
			"/api/battle-sessions" to PermissionRule(RequireBattleSessionsRun::class, BATTLE_SESSIONS_RUN_AUTHORITY),
			"/api/game-data/" to PermissionRule(RequireGameDataAdmin::class, GAME_DATA_ADMIN_AUTHORITY),
		)
		val violations = handlerMapping.handlerMethods.flatMap { (mapping, handlerMethod) ->
			mapping.pathPatternsCondition?.patternValues.orEmpty().mapNotNull { path ->
				val rule = ruleByPrefix.entries.firstOrNull { path.startsWith(it.key) }?.value
					?: return@mapNotNull null
				val declared = AnnotatedElementUtils.hasAnnotation(handlerMethod.beanType, rule.annotation.java) ||
					AnnotatedElementUtils.hasAnnotation(handlerMethod.method, rule.annotation.java)
				if (declared) null else "$path -> missing ${rule.annotation.simpleName}"
			}
		}

		assertThat(violations).isEmpty()
	}

	@Test
	fun `every annotated permission exists in the access node catalog`() {
		val catalogCodes = catalogCodes()
		val annotationAuthorities = setOf(
			SECURITY_ADMIN_AUTHORITY,
			BATTLE_RULES_ADMIN_AUTHORITY,
			BATTLE_SANDBOX_RUN_AUTHORITY,
			BATTLE_SESSIONS_RUN_AUTHORITY,
			GAME_DATA_ADMIN_AUTHORITY,
		)

		assertThat(catalogCodes).containsAll(annotationAuthorities)
	}

	@Test
	fun `published access node contract matches the database catalog`() {
		val publishedCodes = objectMapper.readValue(
			Path.of(System.getProperty("avalon.root-dir"), "docs/contracts/access-node-codes.json").toFile(),
			Array<String>::class.java,
		).toSet()

		assertThat(publishedCodes).isEqualTo(catalogCodes())
	}

	@Test
	fun `published ui consumer contracts only use catalog access nodes`() {
		val catalogCodes = catalogCodes()
		val consumerRoot = Path.of(System.getProperty("avalon.root-dir"), "docs/contracts/consumers")
		val violations = Files.list(consumerRoot).use { paths ->
			paths.sorted().flatMap { path ->
				objectMapper.readValue(path.toFile(), Array<String>::class.java)
					.filterNot(catalogCodes::contains)
					.map { code -> "${path.fileName}: $code" }
					.stream()
			}.toList()
		}

		assertThat(violations).isEmpty()
	}

	private fun catalogCodes(): Set<String> = sqlClient.createQuery(SecurityAccessNode::class) {
		select(table.code)
	}.execute().toSet()

	private data class PermissionRule(
		val annotation: KClass<out Annotation>,
		val authority: String,
	)
}
