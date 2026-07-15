package io.github.lishangbu.system.service

import io.github.lishangbu.common.web.conflict
import io.github.lishangbu.security.entity.SecurityAccessNode
import io.github.lishangbu.security.entity.SecurityRole
import io.github.lishangbu.security.entity.SecurityUser
import io.github.lishangbu.security.entity.accessNodes
import io.github.lishangbu.security.entity.accountNonLocked
import io.github.lishangbu.security.entity.code
import io.github.lishangbu.security.entity.enabled
import io.github.lishangbu.security.entity.id
import io.github.lishangbu.security.entity.roles
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ne
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.springframework.stereotype.Component

/** Serializes RBAC mutations that could remove the final usable system administrator. */
@Component
class EffectiveAdminGuard(
	private val sqlClient: KSqlClient,
) {
	fun ensureUserCanBecomeIneffective(userId: Long) {
		lockAdminAccessNode()
		if (!effectiveAdminExistsExcept(userId)) {
			lastAdminConflict("userId")
		}
	}

	fun ensureUserCanReceiveRoles(user: SecurityUser, roleIds: List<Long>) {
		val adminAccessNodeId = lockAdminAccessNode()
		if (user.enabled && user.accountNonLocked && rolesGrantAdmin(roleIds, adminAccessNodeId)) {
			return
		}
		if (!effectiveAdminExistsExcept(user.id)) {
			lastAdminConflict("roleCodes")
		}
	}

	fun ensureRoleCanReceiveAccessNodes(roleId: Long, accessNodeIds: List<Long>) {
		val adminAccessNodeId = lockAdminAccessNode()
		if (adminAccessNodeId in accessNodeIds) {
			return
		}
		if (!effectiveAdminExistsWithoutRole(roleId)) {
			lastAdminConflict("accessNodeCodes")
		}
	}

	private fun lockAdminAccessNode(): Long =
		sqlClient.createQuery(SecurityAccessNode::class) {
			where(table.code eq ADMIN_ACCESS_NODE)
			select(table.id)
		}.forUpdate().execute().single()

	private fun rolesGrantAdmin(roleIds: List<Long>, adminAccessNodeId: Long): Boolean {
		if (roleIds.isEmpty()) {
			return false
		}
		return sqlClient.createQuery(SecurityRole::class) {
			val accessNode = table.joinList(SecurityRole::accessNodes)
			where(table.id valueIn roleIds)
			where(accessNode.id eq adminAccessNodeId)
			select(table.id)
		}.exists()
	}

	private fun effectiveAdminExistsExcept(userId: Long): Boolean =
		sqlClient.createQuery(SecurityUser::class) {
			val role = table.joinList(SecurityUser::roles)
			val accessNode = role.joinList(SecurityRole::accessNodes)
			where(table.id ne userId)
			where(table.enabled eq true)
			where(table.accountNonLocked eq true)
			where(accessNode.code eq ADMIN_ACCESS_NODE)
			select(table.id)
		}.exists()

	private fun effectiveAdminExistsWithoutRole(roleId: Long): Boolean =
		sqlClient.createQuery(SecurityUser::class) {
			val role = table.joinList(SecurityUser::roles)
			val accessNode = role.joinList(SecurityRole::accessNodes)
			where(role.id ne roleId)
			where(table.enabled eq true)
			where(table.accountNonLocked eq true)
			where(accessNode.code eq ADMIN_ACCESS_NODE)
			select(table.id)
		}.exists()

	private fun lastAdminConflict(field: String): Nothing =
		conflict(field, "At least one enabled and unlocked security administrator must remain")

	private companion object {
		const val ADMIN_ACCESS_NODE = "security:admin"
	}
}
