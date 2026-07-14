package io.github.lishangbu.security.token

import cn.dev33.satoken.dao.SaTokenDao
import cn.dev33.satoken.dao.auto.SaTokenDaoByObjectFollowString
import io.github.lishangbu.security.entity.SecurityTokenState
import io.github.lishangbu.security.entity.expiresAt
import io.github.lishangbu.security.entity.id
import io.github.lishangbu.security.entity.stateKey
import io.github.lishangbu.security.entity.stateValue
import org.babyfish.jimmer.sql.ast.Expression
import org.babyfish.jimmer.sql.ast.LikeMode
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.gt
import org.babyfish.jimmer.sql.kt.ast.expression.isNull
import org.babyfish.jimmer.sql.kt.ast.expression.like
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

/** 使用 Jimmer 持久化 Sa-Token 的全部运行状态。 */
@Component
@Transactional
class JimmerSaTokenDao(
	private val sqlClient: KSqlClient,
) : SaTokenDaoByObjectFollowString {
	override fun get(key: String): String? {
		val state = findState(key) ?: return null
		val expiresAt = state.expiresAt
		return if (expiresAt == null || expiresAt.isAfter(Instant.now())) state.stateValue else null
	}

	override fun set(key: String, value: String, timeout: Long) {
		if (timeout != SaTokenDao.NEVER_EXPIRE && timeout <= 0) {
			delete(key)
			return
		}
		val expiresAt = timeout.toExpiresAt()
		val existing = findState(key)
		if (existing == null) {
			sqlClient.save(
				SecurityTokenState {
					stateKey = key
					stateValue = value
					this.expiresAt = expiresAt
				},
			) { setMode(SaveMode.INSERT_ONLY) }
		} else {
			sqlClient.createUpdate(SecurityTokenState::class) {
				where(table.id eq existing.id)
				set(table.stateValue, value)
				set(table.expiresAt, expiresAt)
			}.execute()
		}
	}

	override fun update(key: String, value: String) {
		sqlClient.createUpdate(SecurityTokenState::class) {
			where(table.stateKey eq key)
			set(table.stateValue, value)
		}.execute()
	}

	override fun delete(key: String) {
		sqlClient.createDelete(SecurityTokenState::class) {
			where(table.stateKey eq key)
		}.execute()
	}

	override fun getTimeout(key: String): Long {
		val state = findState(key) ?: return SaTokenDao.NOT_VALUE_EXPIRE
		val expiresAt = state.expiresAt ?: return SaTokenDao.NEVER_EXPIRE
		val timeout = Duration.between(Instant.now(), expiresAt).seconds
		return if (timeout > 0) timeout else SaTokenDao.NOT_VALUE_EXPIRE
	}

	override fun updateTimeout(key: String, timeout: Long) {
		if (timeout != SaTokenDao.NEVER_EXPIRE && timeout <= 0) {
			delete(key)
			return
		}
		sqlClient.createUpdate(SecurityTokenState::class) {
			where(table.stateKey eq key)
			set(table.expiresAt, timeout.toExpiresAt())
		}.execute()
	}

	@Suppress("UNCHECKED_CAST")
	override fun searchData(prefix: String, keyword: String, start: Int, size: Int, sortType: Boolean): List<String> {
		val now = Instant.now()
		val query = sqlClient.createQuery(SecurityTokenState::class) {
			where(or(table.expiresAt.isNull(), table.expiresAt gt now))
			where(table.stateKey.like(prefix, LikeMode.START))
			where(table.stateKey.like(keyword, LikeMode.ANYWHERE))
			if (sortType) {
				orderBy(table.stateKey)
			} else {
				// Jimmer Kotlin 表达式在运行时实现 Java Expression，降序 API 仍由 Java 契约提供。
				orderBy((table.stateKey as Expression<String>).desc())
			}
			select(table.stateKey)
		}
		return if (size > 0) {
			query.limit(size, start.toLong().coerceAtLeast(0)).execute()
		} else {
			query.execute()
		}
	}

	private fun findState(key: String): SecurityTokenState? =
		sqlClient.createQuery(SecurityTokenState::class) {
			where(table.stateKey eq key)
			select(table)
		}.execute().firstOrNull()

	private fun Long.toExpiresAt(): Instant? =
		if (this == SaTokenDao.NEVER_EXPIRE) null else Instant.now().plusSeconds(this)
}