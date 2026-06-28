package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battlerules.dto.BattleSkillRuleRequest
import io.github.lishangbu.battlerules.dto.BattleSkillRuleResponse
import io.github.lishangbu.battlerules.entity.BattleSkillRule
import io.github.lishangbu.battlerules.entity.affectedByProtect
import io.github.lishangbu.battlerules.entity.damagePolicy
import io.github.lishangbu.battlerules.entity.description
import io.github.lishangbu.battlerules.entity.effectPolicy
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.hitPolicy
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.makesContact
import io.github.lishangbu.battlerules.entity.powderBased
import io.github.lishangbu.battlerules.entity.punchBased
import io.github.lishangbu.battlerules.entity.slicingBased
import io.github.lishangbu.battlerules.entity.skillId
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.battlerules.entity.soundBased
import io.github.lishangbu.battlerules.entity.targetPolicy
import io.github.lishangbu.battlerules.repository.BattleSkillRuleRepository
import io.github.lishangbu.common.web.conflict
import io.github.lishangbu.common.web.mapRows
import io.github.lishangbu.common.web.notFound
import io.github.lishangbu.common.web.searchFilter
import io.github.lishangbu.common.web.validatePage
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.babyfish.jimmer.sql.kt.ast.expression.ne
import org.babyfish.jimmer.sql.kt.ast.expression.or
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 战斗技能规则维护服务。
 *
 * 这个服务是技能进入战斗引擎前的规则入口。基础资料中的技能只告诉我们名称、属性、威力、命中等静态事实，
 * 但现代战斗结算还需要知道目标选择、命中模型、伤害模型、是否接触、是否被保护阻挡等行为标签。
 *
 * Service 不把这些策略实现写死在数据库里，而是校验并保存稳定的 policy code。后续引擎根据 policy code
 * 选择明确的 Kotlin 实现类，因此这里的职责是保证后台维护出来的数据“可被解释”，不是在 CRUD 层模拟战斗。
 */
@Service
class BattleSkillRuleService(
	private val repository: BattleSkillRuleRepository,
	private val sqlClient: KSqlClient,
	private val jdbcTemplate: JdbcTemplate,
) {
	/**
	 * 分页查询技能规则。
	 *
	 * 可选的 `skillId` 用于从基础技能资料跳转到对应规则；`query` 只搜索 policy 字段，避免把规则说明当作稳定索引。
	 */
	@Transactional(readOnly = true)
	fun list(page: Int, size: Int, skillId: Long?, query: String?): Page<BattleSkillRuleResponse> {
		validatePage(page, size)
		val search = searchFilter(query)
		return sqlClient.createQuery(BattleSkillRule::class) {
			skillId?.let { where(table.skillId eq requiredPositiveId(it, "skillId")) }
			search.pattern?.let { pattern ->
				where(or(table.effectPolicy ilike pattern, table.targetPolicy ilike pattern))
			}
			orderBy(table.sortOrder, table.skillId)
			select(table)
		}.fetchPage(page, size).mapRows { it.toResponse() }
	}

	/**
	 * 读取单条技能规则。
	 */
	@Transactional(readOnly = true)
	fun get(id: Long): BattleSkillRuleResponse =
		ruleByIdOrNotFound(id).toResponse()

	/**
	 * 创建技能规则。
	 *
	 * 一个基础技能只能拥有一条主规则。状态附加、能力阶级变化等多值效果由专门子表维护。
	 */
	@Transactional
	fun create(request: BattleSkillRuleRequest): BattleSkillRuleResponse {
		val normalized = request.normalized()
		validateSkillReference(normalized.skillId)
		ensureSkillAvailable(normalized.skillId, null)
		return repository.save(
			BattleSkillRule {
				skillId = normalized.skillId
				effectPolicy = normalized.effectPolicy
				targetPolicy = normalized.targetPolicy
				hitPolicy = normalized.hitPolicy
				damagePolicy = normalized.damagePolicy
				makesContact = normalized.makesContact
				affectedByProtect = normalized.affectedByProtect
				soundBased = normalized.soundBased
				powderBased = normalized.powderBased
				punchBased = normalized.punchBased
				slicingBased = normalized.slicingBased
				description = normalized.description
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 更新技能规则。
	 *
	 * 允许调整 `skillId`，但会重新校验基础技能存在且不会与其它规则冲突。
	 */
	@Transactional
	fun update(id: Long, request: BattleSkillRuleRequest): BattleSkillRuleResponse {
		ruleByIdOrNotFound(id)
		val normalized = request.normalized()
		validateSkillReference(normalized.skillId)
		ensureSkillAvailable(normalized.skillId, id)
		return repository.save(
			BattleSkillRule {
				this.id = id
				skillId = normalized.skillId
				effectPolicy = normalized.effectPolicy
				targetPolicy = normalized.targetPolicy
				hitPolicy = normalized.hitPolicy
				damagePolicy = normalized.damagePolicy
				makesContact = normalized.makesContact
				affectedByProtect = normalized.affectedByProtect
				soundBased = normalized.soundBased
				powderBased = normalized.powderBased
				punchBased = normalized.punchBased
				slicingBased = normalized.slicingBased
				description = normalized.description
				enabled = normalized.enabled
				sortOrder = normalized.sortOrder
			},
		).toResponse()
	}

	/**
	 * 删除技能规则。
	 *
	 * 如果已有状态附加或能力阶级效果引用该规则，数据库级联删除会一并清理从属效果。
	 */
	@Transactional
	fun delete(id: Long) {
		ruleByIdOrNotFound(id)
		repository.deleteById(id)
	}

	private fun ruleByIdOrNotFound(id: Long): BattleSkillRule =
		repository.findNullable(id) ?: notFound("id", "技能规则不存在: $id")

	private fun validateSkillReference(skillId: Long) {
		requireExistingGameDataReference(jdbcTemplate, "game_skill", skillId, "skillId", "技能")
	}

	private fun ensureSkillAvailable(skillId: Long, selfId: Long?) {
		val exists = sqlClient.createQuery(BattleSkillRule::class) {
			where(table.skillId eq skillId)
			selfId?.let { where(table.id ne it) }
			select(table.id)
		}.exists()
		if (exists) {
			conflict("skillId", "该技能已经配置战斗规则: $skillId")
		}
	}

	private fun BattleSkillRuleRequest.normalized(): BattleSkillRuleRequest =
		copy(
			skillId = requiredPositiveId(skillId, "skillId"),
			effectPolicy = requiredPolicyCode(effectPolicy, "effectPolicy"),
			targetPolicy = requiredPolicyCode(targetPolicy, "targetPolicy"),
			hitPolicy = requiredPolicyCode(hitPolicy, "hitPolicy"),
			damagePolicy = requiredPolicyCode(damagePolicy, "damagePolicy"),
			description = optionalText(description, "description", 800),
			sortOrder = requiredIntRange(sortOrder, "sortOrder", 0, 10000),
		)

	private fun BattleSkillRule.toResponse(): BattleSkillRuleResponse =
		BattleSkillRuleResponse(
			id = id,
			skillId = skillId,
			effectPolicy = effectPolicy,
			targetPolicy = targetPolicy,
			hitPolicy = hitPolicy,
			damagePolicy = damagePolicy,
			makesContact = makesContact,
			affectedByProtect = affectedByProtect,
			soundBased = soundBased,
			powderBased = powderBased,
			punchBased = punchBased,
			slicingBased = slicingBased,
			description = description,
			enabled = enabled,
			sortOrder = sortOrder,
		)
}
