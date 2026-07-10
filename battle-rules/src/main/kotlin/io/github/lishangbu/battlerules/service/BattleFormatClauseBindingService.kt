package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleFormatClauseBindingRequest
import io.github.lishangbu.battlerules.dto.BattleFormatClauseBindingResponse
import io.github.lishangbu.battlerules.entity.BattleFormatClauseBinding
import io.github.lishangbu.battlerules.entity.clauseId
import io.github.lishangbu.battlerules.entity.formatId
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.required
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.repository.BattleFormatClauseBindingRepository
import io.github.lishangbu.battlerules.repository.BattleFormatClauseRepository
import io.github.lishangbu.battlerules.repository.BattleFormatRepository
import io.github.lishangbu.common.web.conflict
import io.github.lishangbu.common.web.invalidReference
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.validatePage
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ne
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 战斗赛制条款绑定维护服务。
 *
 * 绑定服务显式校验赛制和条款都存在，并对 `(formatId, clauseId)` 做服务层判重。
 * 数据库唯一约束仍是最终兜底，避免并发请求绕过应用层检查。
 */
@Service
class BattleFormatClauseBindingService(
	private val repository: BattleFormatClauseBindingRepository,
	private val formatRepository: BattleFormatRepository,
	private val clauseRepository: BattleFormatClauseRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(page: Int, size: Int, formatId: Long?, clauseId: Long?): Page<BattleFormatClauseBindingResponse> {
		validatePage(page, size)
		return sqlClient.createQuery(BattleFormatClauseBinding::class) {
			formatId?.let { where(table.formatId eq requiredPositiveId(it, "formatId")) }
			clauseId?.let { where(table.clauseId eq requiredPositiveId(it, "clauseId")) }
			orderBy(table.formatId, table.sortOrder, table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): BattleFormatClauseBindingResponse =
		bindingByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: BattleFormatClauseBindingRequest): BattleFormatClauseBindingResponse {
		val normalized = request.normalized()
		validateReferences(normalized.formatId, normalized.clauseId)
		ensureBindingAvailable(normalized.formatId, normalized.clauseId, null)
		return repository.save(
			BattleFormatClauseBinding {
				formatId = normalized.formatId
				clauseId = normalized.clauseId
				required = normalized.required
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	@Transactional
	fun update(id: Long, request: BattleFormatClauseBindingRequest): BattleFormatClauseBindingResponse {
		bindingByIdOrNotFound(id)
		val normalized = request.normalized()
		validateReferences(normalized.formatId, normalized.clauseId)
		ensureBindingAvailable(normalized.formatId, normalized.clauseId, id)
		return repository.save(
			BattleFormatClauseBinding {
				this.id = id
				formatId = normalized.formatId
				clauseId = normalized.clauseId
				required = normalized.required
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		bindingByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun bindingByIdOrNotFound(id: Long): BattleFormatClauseBinding =
		repository.findNullable(id) ?: notFound("id", "赛制条款绑定不存在: $id")

	private fun validateReferences(formatId: Long, clauseId: Long) {
		if (formatRepository.findNullable(formatId) == null) {
			invalidReference("formatId", "赛制不存在: $formatId")
		}
		if (clauseRepository.findNullable(clauseId) == null) {
			invalidReference("clauseId", "赛制条款不存在: $clauseId")
		}
	}

	private fun ensureBindingAvailable(formatId: Long, clauseId: Long, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleFormatClauseBinding::class) {
			where(table.formatId eq formatId)
			where(table.clauseId eq clauseId)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("clauseId", "该赛制已经绑定此条款")
		}
	}

	private fun BattleFormatClauseBindingRequest.normalized(): BattleFormatClauseBindingRequest =
		copy(
			formatId = requiredPositiveId(formatId, "formatId"),
			clauseId = requiredPositiveId(clauseId, "clauseId"),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)

	private fun BattleFormatClauseBinding.toResponse(): BattleFormatClauseBindingResponse =
		BattleFormatClauseBindingResponse {
			id = this@toResponse.id
			formatId = this@toResponse.formatId
			clauseId = this@toResponse.clauseId
			required = this@toResponse.required
			sortOrder = this@toResponse.sortOrder
		}
}
