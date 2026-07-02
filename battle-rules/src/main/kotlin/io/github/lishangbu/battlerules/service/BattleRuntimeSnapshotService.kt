package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.BattleActionValidator
import io.github.lishangbu.battleengine.BattleActionViolation
import io.github.lishangbu.battleengine.BattleEngine
import io.github.lishangbu.battleengine.BattlePreparationValidator
import io.github.lishangbu.battleengine.BattlePreparationViolation
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battlerules.dto.BattleActionRequest
import io.github.lishangbu.battlerules.dto.BattleActionValidationRequest
import io.github.lishangbu.battlerules.dto.BattleActionValidationResponse
import io.github.lishangbu.battlerules.dto.BattleActionViolationResponse
import io.github.lishangbu.battlerules.dto.BattlePreparationValidationRequest
import io.github.lishangbu.battlerules.dto.BattlePreparationValidationResponse
import io.github.lishangbu.battlerules.dto.BattlePreparationViolationResponse
import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.requiredText
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 战斗运行时入口服务。
 *
 * 该服务是 battle-rules 管理资料和 battle-engine 纯领域模型之间的应用层门面。它负责事务边界、准备阶段校验、
 * 行动校验和对外暴露的运行时调试装配入口；真正的队伍 DTO 到 [BattleInitialState] 转换委托给
 * [BattleInitialStateAssembler]，底层 SQL/Jimmer 读取委托给 [BattleRuntimeDataLookup]。
 *
 * 当前解释的资料约定：
 * - `LEVEL/MAX` 的 `operandNumber` 转换为成员等级上限。
 * - `CREATURE/SKILL/ABILITY/ITEM` + `BAN` 转换为对应禁用 ID 集合。
 * - `species-unique` 条款转换为队伍内种类唯一。
 * - `item-unique` 条款转换为队伍内道具唯一。
 *
 * 未识别的条款或限制会被忽略，便于资料先行维护；真正需要引擎执行的新规则应在读取器或策略映射器中显式补充，
 * 不应让纯引擎回头解析数据库 policy 字符串。
 */
@Service
class BattleRuntimeSnapshotService(
	private val dataLookup: BattleRuntimeDataLookup,
	private val initialStateAssembler: BattleInitialStateAssembler,
	private val effectAssembler: BattleRuntimeEffectAssembler,
) {
	private val preparationValidator = BattlePreparationValidator()
	private val actionValidator = BattleActionValidator()
	private val battleEngine = BattleEngine()

	/**
	 * 按赛制 code 装配运行时快照。
	 */
	@Transactional(readOnly = true)
	fun getByFormatCode(formatCode: String): BattleRuntimeSnapshot {
		return getByFormatCode(formatCode, dataLookup.coreElementIds())
	}

	private fun getByFormatCode(formatCode: String, elementIds: Map<String, Long>): BattleRuntimeSnapshot =
		dataLookup.runtimeSnapshotByFormatCode(formatCode, elementIds)

	/**
	 * 按赛制 code 校验准备阶段队伍。
	 *
	 * 该方法先装配运行时规则快照，再把请求中的轻量队伍资料转换成引擎初始快照，最后调用纯引擎校验器。
	 * 它不查询成员、技能、特性或道具资料表；ID 是否存在应由上层构建队伍时保证，当前校验只判断赛制是否合法。
	 */
	@Transactional(readOnly = true)
	fun validatePreparation(request: BattlePreparationValidationRequest): BattlePreparationValidationResponse {
		val initialState = assembleInitialState(request)
		val violations = preparationValidator.validate(initialState)
		return BattlePreparationValidationResponse(
			valid = violations.isEmpty(),
			violations = violations.map { it.toResponse() },
		)
	}

	/**
	 * 按赛制 code 校验首回合行动提交。
	 *
	 * 该接口和准备阶段校验共享同一套轻量队伍请求。服务会先装配运行时规则快照，再启动一份纯内存战斗状态，
	 * 最后调用 battle-engine 的行动校验器。它不执行回合结算、不消费随机数，也不把校验结果写入数据库。
	 */
	@Transactional(readOnly = true)
	fun validateActions(request: BattleActionValidationRequest): BattleActionValidationResponse {
		val normalized = request.normalized()
		val initialState = assembleInitialState(
			BattlePreparationValidationRequest(
				formatCode = normalized.formatCode,
				sides = normalized.sides,
			),
		)
		val state = battleEngine.start(initialState)
		val violations = actionValidator.validate(state, normalized.actions.map { it.toBattleAction() })
		return BattleActionValidationResponse(
			valid = violations.isEmpty(),
			violations = violations.map { it.toResponse() },
		)
	}

	/**
	 * 把管理端维护的规则资料和一份轻量队伍请求装配为 battle-engine 的启动快照。
	 *
	 * 这个方法是应用层到纯引擎的真正边界：调用方只提供赛制 code、成员 ID、等级、技能、特性和道具 ID，
	 * 服务会从三范式资料表读取基础属性、能力值、技能执行字段、特性效果和道具效果，并把它们冻结成
	 * [BattleInitialState]。返回值不包含 Jimmer 实体，也不保留数据库连接；拿到后可以直接交给 [BattleEngine]
	 * 启动和结算。准备校验、行动校验和后续真实开战入口都应复用这里，避免出现“校验用一套装配、战斗用另一套
	 * 装配”的规则漂移。
	 */
	@Transactional(readOnly = true)
	fun assembleInitialState(request: BattlePreparationValidationRequest): BattleInitialState =
		initialStateAssembler.assemble(request)

	/**
	 * 按基础技能 ID 装配战斗引擎可消费的技能槽。
	 *
	 * 装配分为两层：
	 * - 基础事实来自 `game_skill`：名称、属性、伤害分类、威力、命中、PP 和优先度。
	 * - 战斗规则来自 `battle_skill_rule` 及其子表：多段命中、保护交互、天气命中覆盖、天气威力倍率、
	 *   状态附加、能力阶级变化、技能 HP 效果和全场环境效果。
	 *
	 * 每个启用基础技能都必须有一条启用中的 `battle_skill_rule`。缺规则会被视为资料错误立即拒绝，
	 * 避免运行时把未建模技能静默降级成普通单体攻击。
	 */
	@Transactional(readOnly = true)
	fun skillSlotsBySkillIds(skillIds: List<Long>): List<BattleSkillSlot> {
		if (skillIds.isEmpty()) {
			invalidValue("skillIds", "skillIds 不能为空")
		}
		return skillIds.map { skillId ->
			if (skillId <= 0) {
				invalidValue("skillIds", "skillIds 只能包含正数 ID")
			}
			dataLookup.skillSlotBySkillId(skillId)
		}
	}

	/**
	 * 按基础成员资料和等级装配战斗引擎需要的能力值与属性集合。
	 *
	 * 第一版请求 DTO 尚未携带个体值、努力值和性格，因此这里采用现代对战常用的中性默认：
	 * - 个体值使用 31。
	 * - 努力值使用 0。
	 * - 性格修正使用 1.0。
	 *
	 * 这样得到的数值仍然来自三范式资料表和标准能力公式，不再使用占位 1。后续 DTO 增加个体/努力/性格后，
	 * 只需要扩展该装配函数的输入，不需要改纯引擎模型。
	 */
	@Transactional(readOnly = true)
	fun creatureRuntimeProfile(creatureId: Long, level: Int): BattleCreatureRuntimeProfile =
		dataLookup.creatureRuntimeProfile(creatureId, level)

	/**
	 * 按基础特性 ID 装配战斗引擎可消费的结构化特性效果。
	 *
	 * 这里显式识别 `battle_ability_rule.effect_policy` 中已经有引擎模型承载的策略：
	 * - 低体力时强化草/火/水属性伤害。
	 * - 受到接触类攻击后概率让攻击方麻痹。
	 * - 出场时降低当前对手上场成员攻击阶级。
	 * - 出场时设置现代普通天气。
	 * - 出场时设置现代普通场地。
	 * - 指定天气下修改速度倍率。
	 * - 指定场地下修改速度倍率。
	 * - 指定天气下回合末按最大 HP 固定比例回复。
	 * - 满 HP 承受致命直接伤害时保留 1 HP。
	 * - 间接伤害免疫。
	 * - 技能反作用伤害免疫。
	 * - 被技能击中要害免疫。
	 * - 无视对手伤害公式能力阶级变化。
	 * - 无视对手命中/闪避阶级变化。
	 * - 使用技能时无视目标侧防守特性效果。
	 * - 免疫其它成员使用的声音类技能。
	 * - 阻止对手先制技能影响己方。
	 *
	 * `ground-immunity` 会影响成员是否接地，由 `groundedByAbilityId` 单独装配；它不是伤害或状态 hook，
	 * 因此不塞进 `BattleAbilityEffect` 列表。暂未有引擎结构的策略保持不输出效果，避免用字符串在纯引擎里硬解析。
	 */
	@Transactional(readOnly = true)
	fun abilityEffectsByAbilityId(abilityId: Long?): List<BattleAbilityEffect> =
		effectAssembler.abilityEffectsByAbilityId(abilityId)

	/**
	 * 判断特性是否让成员在现代规则中不被视为接地。
	 *
	 * 这会影响地面属性免疫、场地是否作用、部分粉末/电属性规则等多个 hook。当前资料用独立 policy 表达，
	 * 装配时转换成成员快照上的稳定布尔值，让引擎后续所有接地判断共享同一个事实。
	 */
	@Transactional(readOnly = true)
	fun groundedByAbilityId(abilityId: Long?): Boolean =
		effectAssembler.groundedByAbilityId(abilityId)

	/**
	 * 按基础道具 ID 装配战斗引擎可消费的结构化携带道具效果。
	 *
	 * 当前接入引擎已有模型覆盖的两类策略：回合末按最大 HP 比例回复，以及造成伤害时增伤并按最大 HP 反伤。
	 * 低体力树果会映射为一次性回复；造成伤害后回复道具会映射为按实际伤害量的比例回复；讲究类速度道具会
	 * 映射为速度倍率和技能选择锁定；指定属性伤害提升道具会映射为匹配技能属性时的稳定威力倍率；
	 * 物理/特殊分类强化道具会映射为对应分类的威力倍率；抗性树果会映射为本体受到指定属性伤害时的一次性减伤；
	 * 效果绝佳伤害提升道具会映射为最终伤害倍率；
	 * 获得主要异常状态或临时状态后即时解除的道具会映射为状态治愈效果；天气、场地和屏障延长类道具会映射为
	 * 成功设置对应持续效果时的回合覆盖；满 HP 保命道具会映射为一次性致命伤害保留 1 HP。
	 */
	@Transactional(readOnly = true)
	fun itemEffectsByItemId(itemId: Long?): List<BattleItemEffect> =
		effectAssembler.itemEffectsByItemId(itemId)

	private fun BattlePreparationValidationRequest.normalized(): BattlePreparationValidationRequest =
		copy(
			formatCode = formatCode.requiredText("formatCode", maxLength = 80),
			sides = sides.takeIf { it.isNotEmpty() } ?: invalidValue("sides", "sides 不能为空"),
		)

	private fun BattleActionValidationRequest.normalized(): BattleActionValidationRequest =
		copy(
			formatCode = formatCode.requiredText("formatCode", maxLength = 80),
			sides = sides.takeIf { it.isNotEmpty() } ?: invalidValue("sides", "sides 不能为空"),
			actions = actions.takeIf { it.isNotEmpty() } ?: invalidValue("actions", "actions 不能为空"),
		)

	private fun BattleActionRequest.toBattleAction(): BattleAction {
		val normalizedType = type.requiredText("type", maxLength = 40).uppercase()
		val normalizedActorId = actorId.requiredText("actorId", maxLength = 80)
		val normalizedTargetActorId = targetActorId.requiredText("targetActorId", maxLength = 80)
		return when (normalizedType) {
			"USE_SKILL" -> BattleAction.UseSkill(
				actorId = normalizedActorId,
				skillId = skillId?.takeIf { it > 0 } ?: invalidValue("skillId", "skillId 必须大于 0"),
				targetActorId = normalizedTargetActorId,
			)
			"SWITCH_PARTICIPANT" -> BattleAction.SwitchParticipant(
				actorId = normalizedActorId,
				targetActorId = normalizedTargetActorId,
			)
			else -> invalidValue("type", "type 只支持 USE_SKILL 或 SWITCH_PARTICIPANT")
		}
	}

	private fun BattlePreparationViolation.toResponse(): BattlePreparationViolationResponse =
		BattlePreparationViolationResponse(
			code = code,
			sideId = sideId,
			actorId = actorId,
			resourceId = resourceId,
			message = message,
		)

	private fun BattleActionViolation.toResponse(): BattleActionViolationResponse =
		BattleActionViolationResponse(
			code = code,
			actorId = actorId,
			targetActorId = targetActorId,
			resourceId = resourceId,
			message = message,
		)
}
