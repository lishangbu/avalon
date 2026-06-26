package io.github.lishangbu.system.rbac.accessnode

import io.github.lishangbu.security.entity.SecurityAccessNode
import io.github.lishangbu.security.entity.apiMethod
import io.github.lishangbu.security.entity.apiPattern
import io.github.lishangbu.security.entity.code
import io.github.lishangbu.security.entity.componentKey
import io.github.lishangbu.security.entity.enabled
import io.github.lishangbu.security.entity.icon
import io.github.lishangbu.security.entity.id
import io.github.lishangbu.security.entity.name
import io.github.lishangbu.security.entity.parentId
import io.github.lishangbu.security.entity.path
import io.github.lishangbu.security.entity.sortOrder
import io.github.lishangbu.security.entity.type
import io.github.lishangbu.security.entity.visible
import io.github.lishangbu.system.error.notFound
import io.github.lishangbu.system.page.mapRows
import io.github.lishangbu.system.page.systemFilterValue
import io.github.lishangbu.system.page.systemSearchFilter
import io.github.lishangbu.system.page.validateSystemPage
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 访问节点系统管理服务。
 */
@Service
@ConditionalOnProperty(prefix = "backend.security", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class AccessNodeService(
	private val sqlClient: KSqlClient,
) {
	/**
	 * 分页查询访问节点，供菜单渲染、角色绑定和授权点审计使用。
	 */
	@Transactional(readOnly = true)
	fun listAccessNodes(
		page: Int,
		size: Int,
		query: String?,
		codePrefix: String?,
		type: String?,
		visible: Boolean?,
		enabled: Boolean?,
	): Page<AccessNodeResponse> {
		validateSystemPage(page, size)
		val searchFilter = systemSearchFilter(query)
		val codePrefixFilter = systemFilterValue("codePrefix", codePrefix)
		val typeFilter = systemFilterValue("type", type)?.uppercase()
		return sqlClient.createQuery(SecurityAccessNode::class) {
			searchFilter.pattern?.let { pattern ->
				where(
					or(
						table.code ilike pattern,
						table.name ilike pattern,
					),
				)
			}
			codePrefixFilter?.let { prefix ->
				where(table.code ilike "${prefix.lowercase()}%")
			}
			typeFilter?.let { value ->
				where(table.type eq value)
			}
			visible?.let { value ->
				where(table.visible eq value)
			}
			enabled?.let { value ->
				where(table.enabled eq value)
			}
			orderBy(table.sortOrder, table.code)
			select(table)
		}.fetchPage(page, size)
			.mapRows {
				it.toResponse()
			}
	}

	/**
	 * 查询单个访问节点。
	 */
	@Transactional(readOnly = true)
	fun getAccessNode(accessNodeCode: String): AccessNodeResponse =
		sqlClient.executeQuery(SecurityAccessNode::class, limit = 1) {
			where(table.code eq accessNodeCode)
			select(table)
		}
			.firstOrNull()
			?.toResponse()
			?: notFound("accessNodeCode", "访问节点不存在: $accessNodeCode")

	/**
	 * 将持久化访问节点转换为管理端响应。
	 */
	private fun SecurityAccessNode.toResponse(): AccessNodeResponse =
		AccessNodeResponse(
			id = id,
			code = code,
			name = name,
			type = type,
			parentId = parentId,
			path = path,
			componentKey = componentKey,
			icon = icon,
			sortOrder = sortOrder,
			visible = visible,
			enabled = enabled,
			apiMethod = apiMethod,
			apiPattern = apiPattern,
		)
}
