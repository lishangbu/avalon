package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleFormatRestrictionRequest
import io.github.lishangbu.battlerules.dto.BattleFormatRestrictionResponse
import io.github.lishangbu.battlerules.entity.BattleFormatRestriction
import io.github.lishangbu.battlerules.entity.code
import io.github.lishangbu.battlerules.entity.description
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.formatId
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.name
import io.github.lishangbu.battlerules.entity.operandNumber
import io.github.lishangbu.battlerules.entity.operandText
import io.github.lishangbu.battlerules.entity.restrictionOperator
import io.github.lishangbu.battlerules.entity.restrictionType
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.repository.BattleFormatRepository
import io.github.lishangbu.battlerules.repository.BattleFormatRestrictionRepository
import io.github.lishangbu.common.web.conflict
import io.github.lishangbu.common.web.invalidReference
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
 * 战斗赛制限制维护服务。
 *
 * 限制记录是后续战斗创建前队伍校验的重要来源。Service 只负责结构化限制资料的维护，
 * 不在这里模拟完整战斗校验；真正的限制解释由规则引擎按 `restrictionType` 和 `restrictionOperator` 分派。
 */
@Service
class BattleFormatRestrictionService(
	private val repository: BattleFormatRestrictionRepository,
	private val formatRepository: BattleFormatRepository,
	private val sqlClient: KSqlClient,
) {
	@Transactional(readOnly = true)
	fun list(page: Int, size: Int, query: String?, formatId: Long?): Page<BattleFormatRestrictionResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(BattleFormatRestriction::class) {
			formatId?.let { where(table.formatId eq requiredPositiveId(it, "formatId")) }
			search.pattern?.let { pattern ->
				where(or(table.code ilike pattern, table.name ilike pattern))
			}
			orderBy(table.formatId, table.sortOrder, table.code)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	@Transactional(readOnly = true)
	fun get(id: Long): BattleFormatRestrictionResponse =
		restrictionByIdOrNotFound(id).toResponse()

	@Transactional
	fun create(request: BattleFormatRestrictionRequest): BattleFormatRestrictionResponse {
		val normalized = request.normalized()
		validateFormat(normalized.formatId)
		ensureCodeAvailable(normalized.formatId, normalized.code, null)
		return repository.save(
			BattleFormatRestriction {
				formatId = normalized.formatId
				code = normalized.code
				name = normalized.name
				restrictionType = normalized.restrictionType
				restrictionOperator = normalized.restrictionOperator
				operandText = normalized.operandText
				operandNumber = normalized.operandNumber
				description = normalized.description
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	@Transactional
	fun update(id: Long, request: BattleFormatRestrictionRequest): BattleFormatRestrictionResponse {
		restrictionByIdOrNotFound(id)
		val normalized = request.normalized()
		validateFormat(normalized.formatId)
		ensureCodeAvailable(normalized.formatId, normalized.code, id)
		return repository.save(
			BattleFormatRestriction {
				this.id = id
				formatId = normalized.formatId
				code = normalized.code
				name = normalized.name
				restrictionType = normalized.restrictionType
				restrictionOperator = normalized.restrictionOperator
				operandText = normalized.operandText
				operandNumber = normalized.operandNumber
				description = normalized.description
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	@Transactional
	fun delete(id: Long) {
		restrictionByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun restrictionByIdOrNotFound(id: Long): BattleFormatRestriction =
		repository.findNullable(id) ?: notFound("id", "赛制限制不存在: $id")

	private fun validateFormat(formatId: Long) {
		if (formatRepository.findNullable(formatId) == null) {
			invalidReference("formatId", "赛制不存在: $formatId")
		}
	}

	private fun ensureCodeAvailable(formatId: Long, code: String, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleFormatRestriction::class) {
			where(table.formatId eq formatId)
			where(table.code eq code)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("code", "该赛制下限制 code 已存在: $code")
		}
	}

	private fun BattleFormatRestrictionRequest.normalized(): BattleFormatRestrictionRequest =
		copy(
			formatId = requiredPositiveId(formatId, "formatId"),
			code = code.requiredSlugCode("code"),
			name = name.requiredText("name", maxLength = 80),
			restrictionType = requiredUpperText(restrictionType, "restrictionType", 40),
			restrictionOperator = requiredUpperText(restrictionOperator, "restrictionOperator", 40),
			operandText = optionalText(operandText, "operandText", 120),
			operandNumber = optionalIntRange(operandNumber, "operandNumber", 0, 10000),
			description = optionalText(description, "description", 600),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)

	private fun BattleFormatRestriction.toResponse(): BattleFormatRestrictionResponse =
		BattleFormatRestrictionResponse {
			id = this@toResponse.id
			formatId = this@toResponse.formatId
			code = this@toResponse.code
			name = this@toResponse.name
			restrictionType = this@toResponse.restrictionType
			restrictionOperator = this@toResponse.restrictionOperator
			operandText = this@toResponse.operandText
			operandNumber = this@toResponse.operandNumber
			description = this@toResponse.description
			enabled = this@toResponse.enabled
			sortOrder = this@toResponse.sortOrder
		}
}
