package io.github.lishangbu.system.rbac.role

import io.github.lishangbu.security.entity.SecurityAccessNode
import io.github.lishangbu.security.entity.SecurityRole
import io.github.lishangbu.security.entity.accessNodes
import io.github.lishangbu.security.entity.code
import io.github.lishangbu.security.entity.id
import io.github.lishangbu.security.entity.name
import io.github.lishangbu.security.repository.SecurityRoleRepository
import io.github.lishangbu.common.web.conflict
import io.github.lishangbu.common.web.invalidReference
import io.github.lishangbu.common.web.normalizedAccessNodeCodes
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.requiredText
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.filterValue
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.babyfish.jimmer.sql.kt.ast.expression.valueIn
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * RBAC 角色系统管理服务。
 *
 * 角色主表通过 Jimmer Repository 写入，角色访问节点绑定通过 Jimmer association API 维护。
 */
@Service
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class RoleService(
	private val roleRepository: SecurityRoleRepository,
	private val sqlClient: KSqlClient,
) {
	/**
	 * 分页查询角色及其访问节点 code。
	 */
	@Transactional(readOnly = true)
	fun listRoles(page: Int, size: Int, query: String?, accessNodeCode: String?): Page<RoleResponse> {
		validatePage(page, size)
		val searchFilter = searchFilter(query)
		val accessNodeCodeFilter = filterValue("accessNodeCode", accessNodeCode)
		val rolePage = sqlClient.createQuery(SecurityRole::class) {
			searchFilter.pattern?.let { pattern ->
				where(
					or(
						table.code ilike pattern,
						table.name ilike pattern,
					),
				)
			}
			accessNodeCodeFilter?.let { code ->
				val accessNode = table.joinList(SecurityRole::accessNodes)
				where(accessNode.code eq code)
			}
			orderBy(table.code)
			select(table)
		}
			.fetchPage(page, size)
		val accessNodeCodesByRoleId = accessNodeCodesByRoleId(rolePage.rows.map(SecurityRole::id))
		return rolePage.mapRows { role ->
			role.toResponse(accessNodeCodesByRoleId[role.id].orEmpty())
		}
	}

	/**
	 * 查询单个角色及其访问节点 code。
	 */
	@Transactional(readOnly = true)
	fun getRole(roleId: Long): RoleResponse =
		roleByIdOrNotFound(roleId).toResponse()

	/**
	 * 创建角色并维护角色访问节点关联表。
	 */
	@Transactional
	fun createRole(request: CreateRoleRequest): RoleResponse {
		val code = request.code.requiredSlugCode("code")
		if (roleExists(code)) {
			conflict("code", "role code 已存在")
		}
		val accessNodeIds = resolveAccessNodeIds(request.accessNodeCodes)
		val role = roleRepository.save(
			SecurityRole {
				this.code = code
				name = request.name.requiredText("name", maxLength = 80)
			},
		)
		insertAccessNodeBindings(role.id, accessNodeIds)
		return role.toResponse()
	}

	/**
	 * 更新角色名称并替换访问节点绑定。
	 */
	@Transactional
	fun updateRole(roleId: Long, request: UpdateRoleRequest): RoleResponse {
		val role = roleByIdOrNotFound(roleId)
		val accessNodeIds = resolveAccessNodeIds(request.accessNodeCodes)
		replaceAccessNodeBindings(role.id, accessNodeIds)
		return roleRepository.save(
			SecurityRole {
				id = role.id
				code = role.code
				name = request.name.requiredText("name", maxLength = 80)
			},
		).toResponse()
	}

	private fun roleByIdOrNotFound(roleId: Long): SecurityRole =
		roleRepository.findNullable(roleId)
			?: notFound("roleId", "角色不存在: $roleId")

	private fun insertAccessNodeBindings(roleId: Long, accessNodeIds: List<Long>) {
		sqlClient
			.getAssociations(SecurityRole::accessNodes)
			.insertAllIfAbsent(listOf(roleId), accessNodeIds)
	}

	private fun replaceAccessNodeBindings(roleId: Long, accessNodeIds: List<Long>) {
		sqlClient
			.getAssociations(SecurityRole::accessNodes)
			.replaceAll(listOf(roleId), accessNodeIds)
	}

	private fun resolveAccessNodeIds(accessNodeCodes: List<String>): List<Long> {
		val requestedCodes = accessNodeCodes.normalizedAccessNodeCodes("accessNodeCodes")
		val accessNodesByCode = sqlClient.executeQuery(SecurityAccessNode::class) {
			where(table.code valueIn requestedCodes)
			select(table)
		}.associateBy { it.code }
		val missingCodes = requestedCodes.filterNot(accessNodesByCode::containsKey)
		if (missingCodes.isNotEmpty()) {
			invalidReference("accessNodeCodes", "accessNodeCodes 不存在: ${missingCodes.joinToString()}")
		}
		return requestedCodes.map { accessNodesByCode.getValue(it).id }
	}

	private fun roleExists(code: String): Boolean =
		sqlClient.createQuery(SecurityRole::class) {
			where(table.code eq code)
			select(table.id)
		}.exists()

	private fun SecurityRole.toResponse(accessNodeCodes: List<String> = accessNodeCodes(id)): RoleResponse =
		RoleResponse(
			id = id,
			code = code,
			name = name,
			accessNodeCodes = accessNodeCodes,
		)

	private fun accessNodeCodes(roleId: Long): List<String> =
		sqlClient.executeQuery(SecurityRole::class) {
			val accessNode = table.joinList(SecurityRole::accessNodes)
			where(table.id eq roleId)
			orderBy(accessNode.code)
			select(accessNode.code)
		}

	private fun accessNodeCodesByRoleId(roleIds: List<Long>): Map<Long, List<String>> {
		if (roleIds.isEmpty()) {
			return emptyMap()
		}
		return sqlClient.executeQuery(SecurityRole::class) {
			val accessNode = table.joinList(SecurityRole::accessNodes)
			where(table.id valueIn roleIds)
			orderBy(table.id, accessNode.code)
			select(table.id, accessNode.code)
		}.groupBy({ it._1 }, { it._2 })
	}
}
