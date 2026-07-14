package io.github.lishangbu.security

import io.github.lishangbu.BackendApplication
import io.github.lishangbu.common.web.security.BATTLE_RULES_ADMIN_AUTHORITY
import io.github.lishangbu.common.web.security.BATTLE_SANDBOX_RUN_AUTHORITY
import io.github.lishangbu.common.web.security.BATTLE_SESSIONS_RUN_AUTHORITY
import io.github.lishangbu.common.web.security.GAME_DATA_ADMIN_AUTHORITY
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
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.test.context.ContextConfiguration
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
import tools.jackson.databind.ObjectMapper
import java.nio.file.Files
import java.nio.file.Path

/**
 * 防止 Controller 权限注解、权限目录与 URL 领域边界在独立演进时发生漂移。
 */
@SpringBootTest(
	classes = [BackendApplication::class],
	properties = ["spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.yaml"],
)
@ContextConfiguration(initializers = [SecurityManagementApiPostgresTestContainer::class])
class ApiAuthorizationContractTests(
	@Autowired @Qualifier("requestMappingHandlerMapping") private val handlerMapping: RequestMappingHandlerMapping,
	@Autowired private val sqlClient: KSqlClient,
	@Autowired private val objectMapper: ObjectMapper,
) {
	@Test
	fun `protected api domains declare the expected method security annotation`() {
		val authorityByPrefix = linkedMapOf(
			"/api/system/" to SECURITY_ADMIN_AUTHORITY,
			"/api/battle-rules/" to BATTLE_RULES_ADMIN_AUTHORITY,
			"/api/battle-sandbox" to BATTLE_SANDBOX_RUN_AUTHORITY,
			"/api/battle-sessions" to BATTLE_SESSIONS_RUN_AUTHORITY,
			"/api/game-data/" to GAME_DATA_ADMIN_AUTHORITY,
		)
		val violations = handlerMapping.handlerMethods.flatMap { (mapping, handlerMethod) ->
			mapping.pathPatternsCondition?.patternValues.orEmpty().mapNotNull { path ->
				val authority = authorityByPrefix.entries.firstOrNull { path.startsWith(it.key) }?.value
					?: return@mapNotNull null
				val annotation = AnnotatedElementUtils.findMergedAnnotation(
					handlerMethod.beanType,
					PreAuthorize::class.java,
				) ?: AnnotatedElementUtils.findMergedAnnotation(handlerMethod.method, PreAuthorize::class.java)
				val expected = "hasAuthority('$authority')"
				if (annotation?.value == expected) null else "$path -> ${annotation?.value ?: "missing"}, expected $expected"
			}
		}

		assertThat(violations).isEmpty()
	}

	@Test
	fun `every method security authority exists in the access node catalog`() {
		val catalogCodes = sqlClient.createQuery(SecurityAccessNode::class) {
			select(table.code)
		}.execute().toSet()
		val authorityPattern = Regex("""hasAuthority\('([^']+)'\)""")
		val annotationAuthorities = handlerMapping.handlerMethods.values
			.flatMap { handlerMethod ->
				listOfNotNull(
					AnnotatedElementUtils.findMergedAnnotation(handlerMethod.beanType, PreAuthorize::class.java),
					AnnotatedElementUtils.findMergedAnnotation(handlerMethod.method, PreAuthorize::class.java),
				)
			}
			.flatMap { annotation ->
				authorityPattern.findAll(annotation.value).map { match -> match.groupValues[1] }.toList()
			}
			.toSet()

		assertThat(annotationAuthorities).isNotEmpty()
		assertThat(catalogCodes).containsAll(annotationAuthorities)
	}

	@Test
	fun `published access node contract matches the database catalog`() {
		val catalogCodes = sqlClient.createQuery(SecurityAccessNode::class) {
			select(table.code)
		}.execute().toSet()
		val publishedCodes = objectMapper.readValue(
			Path.of(System.getProperty("avalon.root-dir"), "docs/contracts/access-node-codes.json").toFile(),
			Array<String>::class.java,
		).toSet()

		assertThat(publishedCodes).isEqualTo(catalogCodes)
	}

	@Test
	fun `published ui consumer contracts only use catalog access nodes`() {
		val catalogCodes = sqlClient.createQuery(SecurityAccessNode::class) {
			select(table.code)
		}.execute().toSet()
		val consumerRoot = Path.of(
			System.getProperty("avalon.root-dir"),
			"docs/contracts/consumers",
		)
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
}
