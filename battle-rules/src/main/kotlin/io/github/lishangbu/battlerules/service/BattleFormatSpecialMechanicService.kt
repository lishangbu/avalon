package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleFormatSpecialMechanicRequest
import io.github.lishangbu.battlerules.dto.BattleFormatSpecialMechanicResponse
import io.github.lishangbu.battlerules.entity.BattleFormatSpecialMechanic
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.formatId
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.mechanicId
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.repository.BattleFormatRepository
import io.github.lishangbu.battlerules.repository.BattleFormatSpecialMechanicRepository
import io.github.lishangbu.battlerules.repository.BattleSpecialMechanicRepository
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
 * 战斗赛制特殊机制绑定维护服务。
 *
 * 绑定表允许不同赛制对同一机制做不同开关。Service 明确校验两端资料存在，
 * 并禁止同一赛制重复绑定同一机制，保证后续引擎组装规则包时不需要处理歧义。
 */
@Service
class BattleFormatSpecialMechanicService(
	private val repository: BattleFormatSpecialMechanicRepository,
	private val formatRepository: BattleFormatRepository,
	private val mechanicRepository: BattleSpecialMechanicRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(page: Int, size: Int, formatId: Long?, mechanicId: Long?): Page<BattleFormatSpecialMechanicResponse> {
		validatePage(page, size)
		return sqlClient.createQuery(BattleFormatSpecialMechanic::class) {
			formatId?.let { where(table.formatId eq requiredPositiveId(it, "formatId")) }
			mechanicId?.let { where(table.mechanicId eq requiredPositiveId(it, "mechanicId")) }
			orderBy(table.formatId, table.sortOrder, table.id)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): BattleFormatSpecialMechanicResponse =
		bindingByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: BattleFormatSpecialMechanicRequest): BattleFormatSpecialMechanicResponse {
		val normalized = request.normalized()
		validateReferences(normalized.formatId, normalized.mechanicId)
		ensureBindingAvailable(normalized.formatId, normalized.mechanicId, null)
		return repository.save(
			BattleFormatSpecialMechanic {
				formatId = normalized.formatId
				mechanicId = normalized.mechanicId
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	@Transactional
	fun update(id: Long, request: BattleFormatSpecialMechanicRequest): BattleFormatSpecialMechanicResponse {
		bindingByIdOrNotFound(id)
		val normalized = request.normalized()
		validateReferences(normalized.formatId, normalized.mechanicId)
		ensureBindingAvailable(normalized.formatId, normalized.mechanicId, id)
		return repository.save(
			BattleFormatSpecialMechanic {
				this.id = id
				formatId = normalized.formatId
				mechanicId = normalized.mechanicId
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		bindingByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun bindingByIdOrNotFound(id: Long): BattleFormatSpecialMechanic =
		repository.findNullable(id) ?: notFound("id", "赛制特殊机制绑定不存在: $id")

	private fun validateReferences(formatId: Long, mechanicId: Long) {
		if (formatRepository.findNullable(formatId) == null) {
			invalidReference("formatId", "赛制不存在: $formatId")
		}
		if (mechanicRepository.findNullable(mechanicId) == null) {
			invalidReference("mechanicId", "特殊机制不存在: $mechanicId")
		}
	}

	private fun ensureBindingAvailable(formatId: Long, mechanicId: Long, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleFormatSpecialMechanic::class) {
			where(table.formatId eq formatId)
			where(table.mechanicId eq mechanicId)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("mechanicId", "该赛制已经绑定此特殊机制")
		}
	}

	private fun BattleFormatSpecialMechanicRequest.normalized(): BattleFormatSpecialMechanicRequest =
		copy(
			formatId = requiredPositiveId(formatId, "formatId"),
			mechanicId = requiredPositiveId(mechanicId, "mechanicId"),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)

	private fun BattleFormatSpecialMechanic.toResponse(): BattleFormatSpecialMechanicResponse =
		BattleFormatSpecialMechanicResponse {
			id = this@toResponse.id
			formatId = this@toResponse.formatId
			mechanicId = this@toResponse.mechanicId
			enabled = this@toResponse.enabled
			sortOrder = this@toResponse.sortOrder
		}
}
