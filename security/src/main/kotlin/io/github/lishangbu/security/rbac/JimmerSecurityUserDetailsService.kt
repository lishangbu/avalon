package io.github.lishangbu.security.rbac

import io.github.lishangbu.security.entity.SecurityRole
import io.github.lishangbu.security.entity.SecurityUser
import io.github.lishangbu.security.entity.accessNodes
import io.github.lishangbu.security.entity.accountNonLocked
import io.github.lishangbu.security.entity.code
import io.github.lishangbu.security.entity.displayName
import io.github.lishangbu.security.entity.enabled
import io.github.lishangbu.security.entity.id
import io.github.lishangbu.security.entity.name
import io.github.lishangbu.security.entity.passwordHash
import io.github.lishangbu.security.entity.roles
import io.github.lishangbu.security.entity.username
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

/**
 * 从 RBAC 表读取 Spring Security 用户详情。
 *
 * 认证链路只读取账号、角色和访问节点快照，不承担管理端写入职责；管理写入由 system 模块维护同一组表。
 */
@Service
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class JimmerSecurityUserDetailsService(
	private val sqlClient: KSqlClient,
) : UserDetailsService {
	override fun loadUserByUsername(username: String): UserDetails {
		val user = loadUser(username) ?: throw UsernameNotFoundException(username)

		return SecurityUserPrincipal(
			id = user.id,
			usernameValue = user.username,
			passwordHash = user.passwordHash,
			displayName = user.displayName,
			roles = loadRoles(user.id),
			accessNodes = loadAccessNodes(user.id),
			enabledValue = user.enabled,
			accountNonLockedValue = user.accountNonLocked,
		)
	}

	private fun loadUser(username: String): SecurityUser? =
		sqlClient.executeQuery(SecurityUser::class, limit = 1) {
			where(table.username eq username)
			select(table)
		}.firstOrNull()

	private fun loadRoles(userId: Long): List<UserRole> =
		sqlClient.executeQuery(SecurityUser::class) {
			val role = table.joinList(SecurityUser::roles)
			where(table.id eq userId)
			orderBy(role.code)
			select(role)
		}
			.map { role ->
				UserRole(
					id = role.id,
					code = role.code,
					name = role.name,
				)
			}

	private fun loadAccessNodes(userId: Long): List<UserAccessNode> =
		sqlClient.executeQuery(SecurityUser::class) {
			val role = table.joinList(SecurityUser::roles)
			val accessNode = role.joinList(SecurityRole::accessNodes)
			where(table.id eq userId)
			where(accessNode.enabled eq true)
			orderBy(accessNode.code)
			select(accessNode).distinct()
		}
			.map { accessNode ->
				UserAccessNode(
					id = accessNode.id,
					code = accessNode.code,
					name = accessNode.name,
				)
			}
}
