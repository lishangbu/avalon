package io.github.lishangbu.system.service

import io.github.lishangbu.system.dto.OAuthJwkResponse
import io.github.lishangbu.security.entity.OAuth2Jwk
import io.github.lishangbu.security.entity.active
import io.github.lishangbu.security.entity.id
import io.github.lishangbu.security.entity.keyId
import io.github.lishangbu.security.oauth.OAuth2JwkKeyFactory
import io.github.lishangbu.security.repository.OAuth2JwkRepository
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.asc
import org.babyfish.jimmer.sql.kt.ast.expression.desc
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

private const val JWK_ROTATION_LOCK_NAMESPACE = 0x41564F4E
private const val JWK_ROTATION_LOCK_RESOURCE = 0x4A574B01

/**
 * JWK 系统管理服务。
 *
 * 轮换管理放在 system 模块，签名 key 的读取和初始化仍属于 security 运行时。
 */
@Service
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class OAuthJwkService(
	private val jwkRepository: OAuth2JwkRepository,
	private val sqlClient: KSqlClient,
	private val jwkKeyFactory: OAuth2JwkKeyFactory,
	private val jdbcTemplate: JdbcTemplate,
) {
	/**
	 * 查询 JWK 元数据，响应不包含私钥 JSON。
	 */
	@Transactional(readOnly = true)
	fun listJwks(page: Int, size: Int, query: String?): Page<OAuthJwkResponse> {
		validatePage(page, size)
		val searchFilter = searchFilter(query)
		return sqlClient.createQuery(OAuth2Jwk::class) {
			searchFilter.pattern?.let { pattern ->
				where(table.keyId ilike pattern)
			}
			orderBy(table.active.desc(), table.id.asc())
			select(table)
		}.fetchPage(page, size)
			.mapRows { it.toResponse() }
	}

	/**
	 * 查询单个 JWK 元数据。
	 */
	@Transactional(readOnly = true)
	fun getJwk(keyId: String): OAuthJwkResponse =
		sqlClient.executeQuery(OAuth2Jwk::class, limit = 1) {
			where(table.keyId eq keyId)
			select(table)
		}
			.firstOrNull()
			?.toResponse()
			?: notFound("keyId", "JWK 不存在: $keyId")

	/**
	 * 生成新的活跃 JWK，并停用旧的活跃 key。
	 */
	@Transactional
	fun rotateJwk(): OAuthJwkResponse {
		acquireRotationLock()
		deactivateActiveJwks()

		val rsaKey = jwkKeyFactory.generateRsaJwk()
		return jwkRepository.save(
			OAuth2Jwk {
				keyId = rsaKey.keyID
				jwkJson = rsaKey.toJSONString()
				active = true
			},
		).toResponse()
	}

	/**
	 * 使用 PostgreSQL 事务级 advisory lock 串行化 JWK 轮换。
	 *
	 * 数据库唯一索引仍然是最终保护；这里提前串行化管理端轮换请求，避免并发事务在
	 * “停用旧 key”和“插入新 active key”之间互相撞到唯一 active 约束。
	 */
	private fun acquireRotationLock() {
		jdbcTemplate.execute("select pg_advisory_xact_lock($JWK_ROTATION_LOCK_NAMESPACE, $JWK_ROTATION_LOCK_RESOURCE)")
	}

	/**
	 * 使用 Jimmer 批量更新停用旧的活跃 JWK。
	 */
	private fun deactivateActiveJwks() {
		sqlClient.executeUpdate(OAuth2Jwk::class) {
			where(table.active eq true)
			set(table.active, false)
		}
	}

	/**
	 * 将持久化 JWK 转换为不含私钥材料的响应。
	 */
	private fun OAuth2Jwk.toResponse(): OAuthJwkResponse =
		OAuthJwkResponse(
			id = id,
			keyId = keyId,
			active = active,
		)

}
