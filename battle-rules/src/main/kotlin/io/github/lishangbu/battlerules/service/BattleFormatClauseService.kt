package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleFormatClauseRequest
import io.github.lishangbu.battlerules.dto.BattleFormatClauseResponse
import io.github.lishangbu.battlerules.entity.BattleFormatClause
import io.github.lishangbu.battlerules.entity.clauseType
import io.github.lishangbu.battlerules.entity.code
import io.github.lishangbu.battlerules.entity.description
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.name
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.repository.BattleFormatClauseRepository
import io.github.lishangbu.common.web.conflict
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.requiredSlugCode
import io.github.lishangbu.common.web.requiredText
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.expression.ne
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 战斗赛制条款维护服务。
 *
 * 条款只描述可复用规则开关，不直接挂到赛制上。这样的拆分能让同一个条款被多个赛制共享，
 * 也让管理端在维护条款文案时不会意外改变赛制绑定关系。
 */
@Service
class BattleFormatClauseService(
	private val repository: BattleFormatClauseRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(page: Int, size: Int, query: String?): Page<BattleFormatClauseResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(BattleFormatClause::class) {
			search.pattern?.let { pattern ->
				where(or(table.code ilike pattern, table.name ilike pattern))
			}
			orderBy(table.sortOrder, table.code)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): BattleFormatClauseResponse =
		clauseByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: BattleFormatClauseRequest): BattleFormatClauseResponse {
		val code = request.code.requiredSlugCode("code")
		ensureCodeAvailable(code, null)
		val normalized = request.normalized()
		return repository.save(
			BattleFormatClause {
				this.code = code
				name = normalized.name
				clauseType = normalized.clauseType
				description = normalized.description
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	@Transactional
	fun update(id: Long, request: BattleFormatClauseRequest): BattleFormatClauseResponse {
		clauseByIdOrNotFound(id)
		val code = request.code.requiredSlugCode("code")
		ensureCodeAvailable(code, id)
		val normalized = request.normalized()
		return repository.save(
			BattleFormatClause {
				this.id = id
				this.code = code
				name = normalized.name
				clauseType = normalized.clauseType
				description = normalized.description
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		clauseByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun clauseByIdOrNotFound(id: Long): BattleFormatClause =
		repository.findNullable(id) ?: notFound("id", "赛制条款不存在: $id")

	private fun ensureCodeAvailable(code: String, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleFormatClause::class) {
			where(table.code eq code)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("code", "赛制条款 code 已存在: $code")
		}
	}

	private fun BattleFormatClauseRequest.normalized(): BattleFormatClauseRequest =
		copy(
			name = name.requiredText("name", maxLength = 80),
			clauseType = requiredUpperText(clauseType, "clauseType", 40),
			description = optionalText(description, "description", 600),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)

	private fun BattleFormatClause.toResponse(): BattleFormatClauseResponse =
		BattleFormatClauseResponse(
			id = id,
			code = code,
			name = name,
			clauseType = clauseType,
			description = description,
			enabled = enabled,
			sortOrder = sortOrder,
		)
}
