package io.github.lishangbu.system.service

import io.github.lishangbu.system.dto.SessionMenuNodeResponse
import io.github.lishangbu.system.dto.SessionResponse
import io.github.lishangbu.system.dto.SessionRoleResponse
import io.github.lishangbu.system.dto.SessionUserResponse
import io.github.lishangbu.security.entity.SecurityAccessNode
import io.github.lishangbu.security.entity.SecurityRole
import io.github.lishangbu.security.entity.SecurityUser
import io.github.lishangbu.security.entity.code
import io.github.lishangbu.security.entity.displayName
import io.github.lishangbu.security.entity.enabled
import io.github.lishangbu.security.entity.id
import io.github.lishangbu.security.entity.name
import io.github.lishangbu.security.entity.parentId
import io.github.lishangbu.security.entity.sortOrder
import io.github.lishangbu.security.entity.type
import io.github.lishangbu.security.entity.username
import io.github.lishangbu.security.entity.visible
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
	 * 按当前认证主体读取用户信息，并用 token authority 快照过滤菜单元数据。
	 */
	@Transactional(readOnly = true)
	fun currentSession(authentication: Authentication): SessionResponse {
		val user = userByUsernameOrNotFound(authentication.name)
		val roleCodes = authentication.roleCodes()
		val accessNodeCodes = authentication.accessNodeCodes()
		val roles = loadRoles(roleCodes)
		val accessNodes = loadAccessNodes(accessNodeCodes)
		return SessionResponse(
			user = SessionUserResponse(
				id = user.id,
				username = user.username,
				displayName = user.displayName,
			),
			roles = roles.map { role ->
				SessionRoleResponse(
					code = role.code,
					name = role.name,
				)
			},
			accessNodeCodes = accessNodeCodes,
			menus = buildMenus(accessNodes),
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

	private fun loadAccessNodes(accessNodeCodes: List<String>): List<SecurityAccessNode> {
		if (accessNodeCodes.isEmpty()) {
			return emptyList()
		}
		return sqlClient.executeQuery(SecurityAccessNode::class) {
			where(table.code valueIn accessNodeCodes)
			where(table.enabled eq true)
			orderBy(table.code)
			select(table)
		}
	}

	private fun buildMenus(accessNodes: List<SecurityAccessNode>): List<SessionMenuNodeResponse> {
		val assignedMenuIds = accessNodes
			.filter { it.isMenuNode() }
			.mapTo(linkedSetOf()) { it.id }
		if (assignedMenuIds.isEmpty()) {
			return emptyList()
		}
		val allMenuNodes = loadMenuNodes()
		val menuNodesById = allMenuNodes.associateBy(SecurityAccessNode::id)
		val includedMenuIds = linkedSetOf<Long>()
		assignedMenuIds.forEach { menuId ->
			collectMenuAncestors(menuId, menuNodesById, includedMenuIds)
		}
		return allMenuNodes
			.filter { it.id in includedMenuIds }
			.toMenuTree()
	}

	private fun loadMenuNodes(): List<SecurityAccessNode> =
		sqlClient.executeQuery(SecurityAccessNode::class) {
			where(table.enabled eq true)
			where(table.visible eq true)
			where(table.type valueIn MENU_NODE_TYPES)
			orderBy(table.sortOrder, table.code)
			select(table)
		}

	private fun collectMenuAncestors(
		menuId: Long,
		menuNodesById: Map<Long, SecurityAccessNode>,
		includedMenuIds: MutableSet<Long>,
	) {
		var currentMenuId: Long? = menuId
		while (currentMenuId != null) {
			val menuNode = menuNodesById[currentMenuId] ?: return
			includedMenuIds += menuNode.id
			currentMenuId = menuNode.parentId
		}
	}

	private fun List<SecurityAccessNode>.toMenuTree(): List<SessionMenuNodeResponse> {
		val childrenByParentId = groupBy { it.parentId }
		fun build(parentId: Long?): List<SessionMenuNodeResponse> =
			childrenByParentId[parentId]
				.orEmpty()
				.sortedWith(compareBy<SecurityAccessNode> { it.sortOrder }.thenBy { it.code })
				.map { node ->
					node.toMenuNode(build(node.id))
				}
		return build(null)
	}

	private fun SecurityAccessNode.toMenuNode(children: List<SessionMenuNodeResponse>): SessionMenuNodeResponse =
		SessionMenuNodeResponse(
			code = code,
			name = name,
			type = type,
			path = path,
			componentKey = componentKey,
			icon = icon,
			sortOrder = sortOrder,
			children = children,
		)

	private fun SecurityAccessNode.isMenuNode(): Boolean =
		enabled && visible && type in MENU_NODE_TYPES

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

	private companion object {
		private const val ROLE_AUTHORITY_PREFIX = "ROLE_"
		private val MENU_NODE_TYPES = listOf("DIRECTORY", "ROUTE")
	}
}
