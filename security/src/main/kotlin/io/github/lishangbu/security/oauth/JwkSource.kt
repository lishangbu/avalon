package io.github.lishangbu.security.oauth

import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.JWKSelector
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import io.github.lishangbu.security.entity.OAuth2Jwk
import io.github.lishangbu.security.entity.active
import io.github.lishangbu.security.entity.createdAt
import io.github.lishangbu.security.entity.id
import io.github.lishangbu.security.repository.OAuth2JwkRepository
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.beans.factory.InitializingBean
import java.time.OffsetDateTime
import java.time.ZoneOffset

/**
 * 从数据库读取并维护授权服务器的活跃 JWK。
 *
 * 当前实现保证至少存在一个活跃 RSA key，供 JWT access token 签名和 JWK Set 端点使用。
 */
class JwkSource(
	private val repository: OAuth2JwkRepository,
	private val sqlClient: KSqlClient,
	private val jwkKeyFactory: OAuth2JwkKeyFactory,
) : JWKSource<SecurityContext>, InitializingBean {
	override fun afterPropertiesSet() {
		ensureActiveKey()
	}

	override fun get(jwkSelector: JWKSelector, context: SecurityContext?): List<JWK> =
		jwkSelector.select(activeJwkSet())

	private fun activeJwkSet(): JWKSet {
		ensureActiveKey()
		return JWKSet(
			activeJwks()
				.asSequence()
				.map { JWK.parse(it.jwkJson) }
				.toList(),
		)
	}

	private fun ensureActiveKey() {
		if (hasActiveJwk()) {
			return
		}

		val rsaKey = jwkKeyFactory.generateRsaJwk()
		val now = OffsetDateTime.now(ZoneOffset.UTC)
		repository.save(
			OAuth2Jwk {
				keyId = rsaKey.keyID
				jwkJson = rsaKey.toJSONString()
				active = true
				createdAt = now
				updatedAt = now
			},
		)
	}

	private fun activeJwks(): List<OAuth2Jwk> =
		sqlClient.executeQuery(OAuth2Jwk::class) {
			where(table.active eq true)
			orderBy(table.createdAt)
			select(table)
		}

	private fun hasActiveJwk(): Boolean =
		sqlClient.createQuery(OAuth2Jwk::class) {
			where(table.active eq true)
			select(table.id)
		}.exists()
}
