package io.github.lishangbu.system.service

import io.github.lishangbu.system.dto.SessionResponse
import io.github.lishangbu.system.dto.SessionRoleResponse
import io.github.lishangbu.system.dto.SessionUserResponse
import io.github.lishangbu.security.rbac.BATTLE_RULES_ADMIN_ACCESS_NODE
import io.github.lishangbu.security.rbac.BATTLE_SANDBOX_RUN_ACCESS_NODE
import io.github.lishangbu.security.rbac.BATTLE_SESSIONS_RUN_ACCESS_NODE
import io.github.lishangbu.security.rbac.GAME_DATA_ADMIN_ACCESS_NODE
import io.github.lishangbu.security.rbac.SECURITY_ADMIN_ACCESS_NODE
import io.github.lishangbu.security.entity.SecurityRole
import io.github.lishangbu.security.entity.SecurityUser
import io.github.lishangbu.security.entity.accessNodes
import io.github.lishangbu.security.entity.code
import io.github.lishangbu.security.entity.displayName
import io.github.lishangbu.security.entity.enabled
import io.github.lishangbu.security.entity.id
import io.github.lishangbu.security.entity.name
import io.github.lishangbu.security.entity.roles
import io.github.lishangbu.security.entity.username
import io.github.lishangbu.common.web.notFound
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 当前登录态查询服务。
 */
@Service
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class SessionService(
	private val sqlClient: KSqlClient,
) {
	/**
	 * 按当前认证主体读取用户信息，并用“当前数据库角色权限 + token 已授权 scope”生成权限快照。
	 *
	 * reference token 中保存的是签发瞬间的权限 claim；这里仍然尊重 token 已授权的 scope，
	 * 避免返回超出 token 业务域的权限，同时从数据库实时读取当前角色权限关系。
	 */
	@Transactional(readOnly = true)
	fun currentSession(authentication: Authentication): SessionResponse {
		val user = userByUsernameOrNotFound(authentication.name)
		val roleCodes = authentication.roleCodes()
		val grantedScopeCodes = authentication.grantedScopeCodes()
		val accessNodeCodes = loadScopedAccessNodeCodes(user.id, grantedScopeCodes)
		val roles = loadRoles(roleCodes)
		return SessionResponse(
			user = SessionUserResponse {
				id = user.id
				username = user.username
				displayName = user.displayName
			},
			roles = roles.map { role ->
				SessionRoleResponse(
					code = role.code,
					name = role.name,
				)
			},
			accessNodeCodes = accessNodeCodes,
		)
	}

	private fun userByUsernameOrNotFound(username: String): SecurityUser =
		sqlClient.executeQuery(SecurityUser::class, limit = 1) {
			where(table.username eq username)
			select(table)
		}
			.firstOrNull()
			?: notFound("username", "用户不存在: $username")

	private fun loadRoles(roleCodes: List<String>): List<SecurityRole> {
		if (roleCodes.isEmpty()) {
			return emptyList()
		}
		return sqlClient.executeQuery(SecurityRole::class) {
			where(table.code valueIn roleCodes)
			orderBy(table.code)
			select(table)
		}
	}

	private fun loadScopedAccessNodeCodes(userId: Long, grantedScopeCodes: Set<String>): List<String> {
		if (grantedScopeCodes.isEmpty()) {
			return emptyList()
		}
		return sqlClient.executeQuery(SecurityUser::class) {
			val role = table.joinList(SecurityUser::roles)
			val accessNode = role.joinList(SecurityRole::accessNodes)
			where(table.id eq userId)
			where(accessNode.enabled eq true)
			orderBy(accessNode.code)
			select(accessNode).distinct()
		}
			.asSequence()
			.map { it.code }
			.filter { code ->
				grantedScopeCodes.any { scope -> code == scope || code.isAccessNodeInScope(scope) }
			}
			.distinct()
			.sorted()
			.toList()
	}

	private fun Authentication.roleCodes(): List<String> =
		authorities
			.mapNotNull { it.authority }
			.filter { it.startsWith(ROLE_AUTHORITY_PREFIX) }
			.map { it.removePrefix(ROLE_AUTHORITY_PREFIX) }
			.distinct()
			.sorted()

	private fun Authentication.accessNodeCodes(): List<String> =
		authorities
			.mapNotNull { it.authority }
			.filterNot { it.startsWith(ROLE_AUTHORITY_PREFIX) }
			.distinct()
			.sorted()

	private fun Authentication.grantedScopeCodes(): Set<String> =
		accessNodeCodes()
			.filterTo(linkedSetOf()) { it in SESSION_SCOPE_ACCESS_NODES }

	private fun String.isAccessNodeInScope(scope: String): Boolean =
		when (scope) {
			SECURITY_ADMIN_ACCESS_NODE -> startsWith("system")
			BATTLE_RULES_ADMIN_ACCESS_NODE -> startsWith("battle-rules")
			BATTLE_SANDBOX_RUN_ACCESS_NODE -> startsWith("battle-sandbox")
			BATTLE_SESSIONS_RUN_ACCESS_NODE -> startsWith("battle-sessions")
			GAME_DATA_ADMIN_ACCESS_NODE -> startsWith("game-data")
			else -> false
		}

	private companion object {
		private const val ROLE_AUTHORITY_PREFIX = "ROLE_"
		private val SESSION_SCOPE_ACCESS_NODES = setOf(
			SECURITY_ADMIN_ACCESS_NODE,
			BATTLE_RULES_ADMIN_ACCESS_NODE,
			BATTLE_SANDBOX_RUN_ACCESS_NODE,
			BATTLE_SESSIONS_RUN_ACCESS_NODE,
			GAME_DATA_ADMIN_ACCESS_NODE,
		)
	}
}
