package io.github.lishangbu.security.rbac

import io.github.lishangbu.security.entity.SecurityRole
import io.github.lishangbu.security.entity.SecurityUser
import io.github.lishangbu.security.entity.accessNodes
import io.github.lishangbu.security.entity.code
import io.github.lishangbu.security.entity.enabled
import io.github.lishangbu.security.entity.id
import io.github.lishangbu.security.entity.name
import io.github.lishangbu.security.entity.roles
import io.github.lishangbu.security.entity.username
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 集中读取 Sa-Token 登录和授权所需的 Jimmer RBAC 快照。
 */
@Service
class JimmerSaTokenRbacService(
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun findUserByUsername(username: String): SecurityUser? =
		sqlClient.executeQuery(SecurityUser::class, limit = 1) {
			where(table.username eq username)
			select(table)
		}.firstOrNull()

	@Transactional(readOnly = true)
	fun findUserById(userId: Long): SecurityUser? =
		sqlClient.executeQuery(SecurityUser::class, limit = 1) {
			where(table.id eq userId)
			select(table)
		}.firstOrNull()

	@Transactional(readOnly = true)
	fun loadRoles(userId: Long): List<UserRole> =
		sqlClient.executeQuery(SecurityUser::class) {
			val role = table.joinList(SecurityUser::roles)
			where(table.id eq userId)
			orderBy(role.code)
			select(role)
		}.map { role -> UserRole(role.id, role.code, role.name) }

	@Transactional(readOnly = true)
	fun loadAccessNodes(userId: Long): List<UserAccessNode> =
		sqlClient.executeQuery(SecurityUser::class) {
			val role = table.joinList(SecurityUser::roles)
			val accessNode = role.joinList(SecurityRole::accessNodes)
			where(table.id eq userId)
			where(accessNode.enabled eq true)
			orderBy(accessNode.code)
			select(accessNode).distinct()
		}.map { accessNode -> UserAccessNode(accessNode.id, accessNode.code, accessNode.name) }
}
