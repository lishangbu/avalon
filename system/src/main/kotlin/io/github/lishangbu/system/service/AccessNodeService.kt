package io.github.lishangbu.system.service

import io.github.lishangbu.system.dto.AccessNodeResponse
import io.github.lishangbu.security.entity.SecurityAccessNode
import io.github.lishangbu.security.entity.code
import io.github.lishangbu.security.entity.enabled
import io.github.lishangbu.security.entity.id
import io.github.lishangbu.security.entity.name
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.filterValue
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
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
	 * 分页查询访问权限，供角色绑定和授权点审计使用。
	 */
	@Transactional(readOnly = true)
	fun listAccessNodes(
		page: Int,
		size: Int,
		query: String?,
		codePrefix: String?,
		enabled: Boolean?,
	): Page<AccessNodeResponse> {
		validatePage(page, size)
		val searchFilter = searchFilter(query)
		val codePrefixFilter = filterValue("codePrefix", codePrefix)
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
			enabled?.let { value ->
				where(table.enabled eq value)
			}
			orderBy(table.code)
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
		AccessNodeResponse {
			id = this@toResponse.id
			code = this@toResponse.code
			name = this@toResponse.name
			enabled = this@toResponse.enabled
		}
}
