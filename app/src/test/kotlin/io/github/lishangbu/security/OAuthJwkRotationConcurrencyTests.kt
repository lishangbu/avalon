package io.github.lishangbu.security

import io.github.lishangbu.BackendApplication
import io.github.lishangbu.security.entity.OAuth2Jwk
import io.github.lishangbu.security.entity.active
import io.github.lishangbu.security.entity.keyId
import io.github.lishangbu.system.oauth.jwk.OAuthJwkResponse
import io.github.lishangbu.system.oauth.jwk.OAuthJwkService
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest(
	classes = [BackendApplication::class],
	properties = [
		"spring.liquibase.change-log=classpath:/db/changelog/db.changelog-master.yaml",
	],
)
@ContextConfiguration(initializers = [SecurityManagementApiPostgresTestContainer::class])
/**
 * 验证 JWK 轮换在并发和事务回滚场景下始终保留唯一 active key。
 */
class OAuthJwkRotationConcurrencyTests(
	@Autowired private val oauthJwkService: OAuthJwkService,
	@Autowired private val sqlClient: KSqlClient,
	@Autowired transactionManager: PlatformTransactionManager,
) {
	private val transactionTemplate = TransactionTemplate(transactionManager)

	@Test
	fun `concurrent jwk rotations keep exactly one active key`() {
		val initialKeyId = oauthJwkService.rotateJwk().keyId
		val rotationCount = 6
		val executor = Executors.newFixedThreadPool(rotationCount)
		val ready = CountDownLatch(rotationCount)
		val start = CountDownLatch(1)

		try {
			val futures = (1..rotationCount).map {
				executor.submit<OAuthJwkResponse> {
					ready.countDown()
					check(start.await(10, TimeUnit.SECONDS)) { "JWK rotation start latch timed out" }
					oauthJwkService.rotateJwk()
				}
			}

			assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue()
			start.countDown()

			val rotatedKeyIds = futures.map { it.get(30, TimeUnit.SECONDS).keyId }
			val activeKeyIds = activeJwkKeyIds()

			assertThat(rotatedKeyIds)
				.hasSize(rotationCount)
				.doesNotContain(initialKeyId)
				.doesNotHaveDuplicates()
			assertThat(activeKeyIds).hasSize(1)
			assertThat(rotatedKeyIds).contains(activeKeyIds.single())
		} finally {
			executor.shutdownNow()
		}
	}

	@Test
	fun `jwk rotation rolls back deactivation and insertion together`() {
		val initialKeyId = oauthJwkService.rotateJwk().keyId

		val error = catchThrowable {
			transactionTemplate.executeWithoutResult {
				val rotated = oauthJwkService.rotateJwk()
				assertThat(rotated.keyId).isNotEqualTo(initialKeyId)
				throw IllegalStateException("force rollback")
			}
		}

		assertThat(error).isInstanceOf(IllegalStateException::class.java)
		assertThat(activeJwkKeyIds()).containsExactly(initialKeyId)
	}

	private fun activeJwkKeyIds(): List<String> =
		sqlClient.executeQuery(OAuth2Jwk::class) {
			where(table.active eq true)
			orderBy(table.keyId)
			select(table.keyId)
		}
}
