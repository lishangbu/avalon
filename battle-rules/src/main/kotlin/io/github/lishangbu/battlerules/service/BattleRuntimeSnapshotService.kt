package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.BattleActionValidator
import io.github.lishangbu.battleengine.BattleActionViolation
import io.github.lishangbu.battleengine.BattleEngine
import io.github.lishangbu.battleengine.BattlePreparationValidator
import io.github.lishangbu.battleengine.BattlePreparationViolation
import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleEnvironment
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderEffect
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderKind
import io.github.lishangbu.battleengine.model.BattleInitialState
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleRandomTraceEntry
import io.github.lishangbu.battleengine.model.BattleResult
import io.github.lishangbu.battleengine.model.BattleSide
import io.github.lishangbu.battleengine.model.BattleSideDamageReduction
import io.github.lishangbu.battleengine.model.BattleSideDamageReductionKind
import io.github.lishangbu.battleengine.model.BattleSideEntryHazard
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardKind
import io.github.lishangbu.battleengine.model.BattleSideProtectionKind
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifier
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifierKind
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleState
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battleengine.random.BattleRandom
import io.github.lishangbu.battleengine.random.RecordingBattleRandom
import io.github.lishangbu.battlerules.dto.BattleActionRequest
import io.github.lishangbu.battlerules.dto.BattleActionValidationRequest
import io.github.lishangbu.battlerules.dto.BattleActionValidationResponse
import io.github.lishangbu.battlerules.dto.BattleActionViolationResponse
import io.github.lishangbu.battlerules.dto.BattlePreparationValidationRequest
import io.github.lishangbu.battlerules.dto.BattlePreparationValidationResponse
import io.github.lishangbu.battlerules.dto.BattlePreparationViolationResponse
import io.github.lishangbu.battlerules.dto.BattleSandboxStateSnapshot
import io.github.lishangbu.battlerules.dto.BattleSandboxTurnParticipant
import io.github.lishangbu.battlerules.dto.BattleSandboxTurnRequest
import io.github.lishangbu.battlerules.dto.BattleSandboxTurnResponse
import io.github.lishangbu.battlerules.dto.BattleSandboxTurnSkillSlot
import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.requiredText
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Locale
import java.util.SplittableRandom
import kotlin.reflect.full.memberProperties

/**
 * 战斗运行时入口服务。
 *
 * 该服务是 battle-rules 管理资料和 battle-engine 纯领域模型之间的应用层门面。它负责事务边界、准备阶段校验、
 * 行动校验和对外暴露的运行时调试装配入口；真正的队伍 DTO 到 [BattleInitialState] 转换委托给
 * [BattleInitialStateAssembler]，底层 Jimmer 读取委托给 [BattleRuntimeDataLookup]。
 *
 * 当前解释的资料约定：
 * - `level-flattened` 条款配合 `defaultLevel` 把参战成员等级和能力值冻结到赛制默认等级。
 * - `LEVEL/MAX` 的 `operandNumber` 转换为成员等级上限，并和等级拉平默认等级做自洽校验。
 * - `TEAM/SELECT_COUNT` 优先转换为运行态参战人数；没有出战人数限制时才退回 `TEAM/LIMIT_COUNT` 或赛制登记人数。
 * - `CREATURE/SKILL/ABILITY/ITEM` + `BAN` 转换为对应禁用 ID 集合。
 * - `species-unique` 条款转换为队伍内种类唯一。
 * - `item-unique` 条款转换为队伍内道具唯一。
 * - `team-preview` 属于开战前选队流程，运行态只消费已经选好的参战成员。
 *
 * 启用中的未知条款或限制会被运行态读取器拒绝，避免后台显示“规则已启用”但战斗引擎没有执行。真正需要引擎执行
 * 的新规则应在读取器或策略映射器中显式补充，不应让纯引擎回头解析数据库 policy 字符串。
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
	private val ruleHitMapper = BattleSandboxRuleHitMapper()

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
		requireRuntimePreparation(initialState)
		val state = battleEngine.start(initialState)
		val violations = actionValidator.validate(state, normalized.actions.map { it.toBattleAction() })
		return BattleActionValidationResponse(
			valid = violations.isEmpty(),
			violations = violations.map { it.toResponse() },
		)
	}

	/**
	 * 在内存中结算一回合沙盒战斗。
	 *
	 * 沙盒复用准备阶段装配、行动校验和正式 [BattleEngine]，所以它展示的是当前资料与规则真实能跑出的结果。
	 * 连续回合不创建服务端对局：响应中的 [BattleSandboxStateSnapshot] 是完整的跨回合运行态，调用方下一次原样
	 * 带回即可。规则、技能定义和基础能力值每次仍从资料表重新装配，再叠加快照中的 HP、PP、异常、天气、场地
	 * 等运行态，避免前端持有过期规则对象。
	 */
	@Transactional(readOnly = true)
	fun resolveSandboxTurn(request: BattleSandboxTurnRequest): BattleSandboxTurnResponse {
		val normalized = BattleActionValidationRequest(
			formatCode = request.formatCode,
			sides = request.sides,
			actions = request.actions,
		).normalized()
		val initialState = assembleInitialState(
			BattlePreparationValidationRequest(
				formatCode = normalized.formatCode,
				sides = normalized.sides,
			),
		)
		requireRuntimePreparation(initialState)
		val current = request.state?.let { restoreSandboxState(initialState, it) } ?: battleEngine.start(initialState)
		if (current.result != null) {
			invalidValue("state", "战斗已经结束，请重置后重新开始")
		}
		val actions = normalized.actions.map { it.toBattleAction() }
		val previousEvents = request.state?.events ?: emptyList()
		val previousTurns = request.state?.turns ?: emptyList()
		val violations = actionValidator.validate(current, actions)
		if (violations.isNotEmpty()) {
			return current.toSandboxResponse(
				resolved = false,
				violations = violations.map { it.toResponse() },
				randomTrace = emptyList(),
				previousEvents = previousEvents,
				previousTurns = previousTurns,
			)
		}
		val random = RecordingBattleRandom(SeededBattleRandom(request.randomSeed))
		val resolved = battleEngine.resolveTurn(current, actions, random)
		return resolved.toSandboxResponse(
			resolved = true,
			violations = emptyList(),
			randomTrace = random.trace(),
			previousEvents = previousEvents,
			previousTurns = previousTurns,
			submittedActions = normalized.actions,
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
	 * 为服务端权威 Battle Session 装配并校验冻结初始状态。
	 *
	 * Session 创建不能像校验接口一样返回“可运行但带违规项”的对象；只有通过完整准备校验的状态才能离开
	 * 资料装配事务并进入纯内存 Session Runtime。
	 */
	@Transactional(readOnly = true)
	fun assembleBattleSessionInitialState(request: BattlePreparationValidationRequest): BattleInitialState {
		val initialState = assembleInitialState(request.normalized())
		requireRuntimePreparation(initialState)
		return initialState
	}

	/**
	 * 在应用层进入 battle-engine 前执行准备阶段保护。
	 *
	 * [BattleEngine.start] 自身已经会 fail-fast 拒绝非法初始状态，这是纯引擎的最后一道不变量保护；但接口层不能让
	 * Kotlin `require` 抛出的 [IllegalArgumentException] 泄漏成通用 500。这里复用结构化准备校验结果，在行动校验
	 * 和沙盒结算两个“会启动引擎”的入口提前转换为稳定的 `ApiException`，让前端拿到明确的 `sides` 字段错误。
	 * 完整的逐项违规列表仍由 `validatePreparation` 返回；这里的职责只是保证运行时入口不会以非法队伍开局。
	 */
	private fun requireRuntimePreparation(initialState: BattleInitialState) {
		val violations = preparationValidator.validate(initialState)
		if (violations.isNotEmpty()) {
			invalidValue(
				"sides",
				"准备阶段队伍不合法：${violations.joinToString(separator = "；") { it.message }}",
			)
		}
	}

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
	 * 该公开调试入口只接收成员资料 ID 和等级，因此继续采用现代对战常用的中性默认：
	 * - 个体值使用 31。
	 * - 努力值使用 0。
	 * - 性格修正使用 1.0。
	 *
	 * 准备校验、行动校验和沙盒入口会通过 [BattlePreparationValidationRequest] 携带完整能力配置；本方法保留默认
	 * 画像，便于管理端或测试快速确认资料表中的基础能力是否能按标准公式折算。
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
	 * 因此不塞进 `BattleAbilityEffect` 列表。其它启用中的未知策略会被装配器直接拒绝，避免资料拼写错误被静默吞掉。
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
		val normalizedType = type.requiredText("type", maxLength = 40).uppercase(Locale.ROOT)
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
		BattlePreparationViolationResponse {
			code = this@toResponse.code
			sideId = this@toResponse.sideId
			actorId = this@toResponse.actorId
			resourceId = this@toResponse.resourceId
			message = this@toResponse.message
		}

	private fun BattleActionViolation.toResponse(): BattleActionViolationResponse =
		BattleActionViolationResponse {
			code = this@toResponse.code
			actorId = this@toResponse.actorId
			targetActorId = this@toResponse.targetActorId
			resourceId = this@toResponse.resourceId
			message = this@toResponse.message
		}

	/**
	 * 从沙盒快照恢复 battle-engine 可继续结算的状态。
	 *
	 * 恢复时先用当前资料表重新启动一份基线状态，再用快照覆盖跨回合运行态。这样做比把完整 [BattleState] JSON
	 * 直接交给前端更小，也更适合生产调试：资料或规则修正后，下一回合会立刻使用后端最新装配逻辑；同时 HP、PP、
	 * 异常、天气、场地、屏障和锁招等战斗事实仍然保持连续。
	 */
	private fun restoreSandboxState(
		initialState: BattleInitialState,
		snapshot: BattleSandboxStateSnapshot,
	): BattleState =
		try {
			val baseline = battleEngine.start(initialState)
			snapshot.validateSandboxSnapshot(baseline)
			BattleState(
				format = baseline.format,
				rules = baseline.rules,
				environment = snapshot.environment.toBattleEnvironment(),
				sides = baseline.sides.map { side -> side.restoreSandboxSide(snapshot.requiredSide(side.sideId)) },
				turnNumber = snapshot.turnNumber,
				events = emptyList(),
				result = snapshot.result?.let { BattleResult(it.winningSideId, it.reason) },
			)
		} catch (_: IllegalArgumentException) {
			invalidValue("state", "state 不是合法沙盒快照，请重置后重新开始")
		}

	/**
	 * 校验管理端带回的沙盒快照是否自洽。
	 *
	 * 沙盒状态仍然是客户端携带的诊断材料，不是正式对局存档。因此这里不做签名验真，也不把它升级成安全边界；
	 * 但恢复运行态前必须先拦住明显破损或被手动改坏的快照，例如回合号跳跃、回合记录缺失、事件片段和累计事件
	 * 数量对不上、随机 trace 序号不连续等。这样错误会以字段校验失败返回给管理端，而不是进入引擎后变成更难
	 * 定位的状态机异常。
	 */
	private fun BattleSandboxStateSnapshot.validateSandboxSnapshot(baseline: BattleState) {
		if (turnNumber < 0) {
			invalidValue("state", "state 回合号不能为负数")
		}
		validateSandboxRosterShape(baseline)
		val expectedTurnNumbers = (1..turnNumber).toList()
		if (turns.map { it.turnNumber } != expectedTurnNumbers) {
			invalidValue("state", "state 回合记录必须从 1 连续到当前回合")
		}
		if (events.any { it.turnNumber !in 0..turnNumber }) {
			invalidValue("state", "state 事件回合号超出当前回合范围")
		}
		turns.forEach { turn ->
			turn.validateSandboxTurnRecord()
			val accumulatedEventCount = events.count { it.turnNumber == turn.turnNumber }
			if (accumulatedEventCount != turn.events.size) {
				invalidValue("state", "state 回合事件片段与累计事件流不一致")
			}
		}
	}

	/**
	 * 校验沙盒快照中的队伍骨架仍然等于当前请求重新装配出的基线队伍。
	 *
	 * 沙盒续算允许前端带回 HP、PP、异常、天气、场地等“会随战斗变化的运行态”，但不允许前端把单打改成双打、
	 * 把某一侧悄悄塞入额外成员，或把当前上场数量改成不符合赛制的形状。真正的队伍与规则事实仍由本次请求的
	 * `formatCode` 和 `sides` 从数据库重新装配；这里把快照中的 sideId、actorId 集合和 activeActorIds 数量
	 * 锁回基线，避免畸形 JSON 绕过 [io.github.lishangbu.battleengine.model.BattleInitialState] 的启动校验后
	 * 直接进入 [BattleState]。
	 */
	private fun BattleSandboxStateSnapshot.validateSandboxRosterShape(baseline: BattleState) {
		if (sides.map { it.sideId }.toSet() != baseline.sides.map { it.sideId }.toSet()) {
			invalidValue("state", "state 双方队伍必须与当前请求一致")
		}
		baseline.sides.forEach { baselineSide ->
			val snapshotSide = requiredSide(baselineSide.sideId)
			val snapshotActorIds = snapshotSide.participants.map { it.actorId }
			val baselineActorIds = baselineSide.participants.map { it.actorId }
			if (snapshotActorIds.toSet() != baselineActorIds.toSet()) {
				invalidValue("state", "state 成员集合必须与当前请求一致")
			}
			if (snapshotActorIds.toSet().size != snapshotActorIds.size) {
				invalidValue("state", "state 同一侧不能包含重复成员")
			}
			if (snapshotSide.activeActorIds.size != baseline.format.activeParticipantsPerSide) {
				invalidValue("state", "state 上场成员数量必须符合赛制")
			}
			if (snapshotSide.activeActorIds.toSet().size != snapshotSide.activeActorIds.size) {
				invalidValue("state", "state 上场成员不能重复")
			}
			if (snapshotSide.activeActorIds.any { it !in baselineActorIds }) {
				invalidValue("state", "state 上场成员必须属于当前队伍")
			}
		}
	}

	private fun BattleSandboxStateSnapshot.TurnRecord.validateSandboxTurnRecord() {
		if (turnNumber <= 0) {
			invalidValue("state", "state 回合记录序号必须大于 0")
		}
		if (actions.isEmpty()) {
			invalidValue("state", "state 回合记录必须包含已提交行动")
		}
		if (actions.map { it.actorId }.toSet().size != actions.size) {
			invalidValue("state", "state 同一回合不能包含重复行动成员")
		}
		actions.forEach { it.toBattleAction() }
		if (randomTrace.map { it.sequence } != (1..randomTrace.size).toList()) {
			invalidValue("state", "state 随机轨迹序号必须从 1 连续递增")
		}
		if (events.any { it.turnNumber != turnNumber }) {
			invalidValue("state", "state 回合记录只能包含自身回合事件")
		}
	}

	private fun BattleSandboxStateSnapshot.requiredSide(sideId: String): BattleSandboxStateSnapshot.Side =
		sides.firstOrNull { it.sideId == sideId }
			?: invalidValue("state", "state 与当前队伍不匹配，请重置后重新开始")

	private fun BattleSandboxStateSnapshot.Side.requiredParticipant(actorId: String): BattleSandboxStateSnapshot.Participant =
		participants.firstOrNull { it.actorId == actorId }
			?: invalidValue("state", "state 与当前成员不匹配，请重置后重新开始")

	private fun BattleSide.restoreSandboxSide(snapshot: BattleSandboxStateSnapshot.Side): BattleSide =
		copy(
			activeActorIds = snapshot.activeActorIds,
			participants = participants.map { participant -> participant.restoreSandboxParticipant(snapshot.requiredParticipant(participant.actorId)) },
			damageReductions = snapshot.damageReductions.map { it.toBattleSideDamageReduction() },
			speedModifiers = snapshot.speedModifiers.map { it.toBattleSideSpeedModifier() },
			entryHazards = snapshot.entryHazards.map { it.toBattleSideEntryHazard() },
		)

	private fun BattleParticipant.restoreSandboxParticipant(snapshot: BattleSandboxStateSnapshot.Participant): BattleParticipant =
		copy(
			currentHp = snapshot.currentHp,
			elementIds = snapshot.elementIds.takeIf { it.isNotEmpty() }?.toSet() ?: elementIds,
			grounded = snapshot.grounded,
			majorStatus = snapshot.majorStatus?.toEnumValue<BattleMajorStatus>("state.majorStatus"),
			statStages = snapshot.statStages.mapKeys { (stat, _) -> stat.toEnumValue<BattleStat>("state.statStages") },
			skillSlots = skillSlots.map { slot -> slot.restoreSandboxSkillSlot(snapshot.skillSlots) },
			activeSkillActionCount = snapshot.activeSkillActionCount,
			weightReduction = snapshot.weightReduction,
			protectionChain = snapshot.protectionChain,
			fatalDamageEndureSkillId = snapshot.fatalDamageEndureSkillId,
			badPoisonCounter = snapshot.badPoisonCounter,
			sleepTurnsRemaining = snapshot.sleepTurnsRemaining,
			chargingSkillId = snapshot.chargingSkillId,
			chargingTargetActorId = snapshot.chargingTargetActorId,
			chargingTurnsRemaining = snapshot.chargingTurnsRemaining,
			rechargeTurnsRemaining = snapshot.rechargeTurnsRemaining,
			flinched = snapshot.flinched,
			confusionTurnsRemaining = snapshot.confusionTurnsRemaining,
			healBlockTurnsRemaining = snapshot.healBlockTurnsRemaining,
			tauntTurnsRemaining = snapshot.tauntTurnsRemaining,
			disabledSkillId = snapshot.disabledSkillId,
			disabledSkillTurnsRemaining = snapshot.disabledSkillTurnsRemaining,
			tormented = snapshot.tormented,
			boundByActorId = snapshot.boundByActorId,
			bindingTurnsRemaining = snapshot.bindingTurnsRemaining,
			leechSeedSourceSideId = snapshot.leechSeedSourceSideId,
			leechSeedSourceActiveIndex = snapshot.leechSeedSourceActiveIndex,
			lastSuccessfulSkillId = snapshot.lastSuccessfulSkillId,
			accuracyLockTargetActorId = snapshot.accuracyLockTargetActorId,
			accuracyLockTurnsRemaining = snapshot.accuracyLockTurnsRemaining,
			lockedMoveSkillId = snapshot.lockedMoveSkillId,
			lockedMoveTargetActorId = snapshot.lockedMoveTargetActorId,
			lockedMoveTurnsRemaining = snapshot.lockedMoveTurnsRemaining,
			lockedMoveConfusesOnEnd = snapshot.lockedMoveConfusesOnEnd,
			choiceLockedSkillId = snapshot.choiceLockedSkillId,
			substituteHp = snapshot.substituteHp,
		)

	private fun BattleSkillSlot.restoreSandboxSkillSlot(
		snapshots: List<BattleSandboxStateSnapshot.SkillSlot>,
	): BattleSkillSlot {
		val snapshot = snapshots.firstOrNull { it.skillId == skillId }
			?: invalidValue("state", "state 与当前技能槽不匹配，请重置后重新开始")
		return copy(remainingPp = snapshot.remainingPp)
	}

	private fun BattleSandboxStateSnapshot.Environment.toBattleEnvironment(): BattleEnvironment =
		BattleEnvironment(
			weather = weather.toEnumValue("state.weather"),
			weatherTurnsRemaining = weatherTurnsRemaining,
			terrain = terrain.toEnumValue("state.terrain"),
			terrainTurnsRemaining = terrainTurnsRemaining,
			fieldSpeedOrderEffect = fieldSpeedOrderEffect?.let {
				BattleFieldSpeedOrderEffect(
					kind = it.kind.toEnumValue<BattleFieldSpeedOrderKind>("state.fieldSpeedOrderEffect.kind"),
					turnsRemaining = it.turnsRemaining,
				)
			},
		)

	private fun BattleSandboxStateSnapshot.DamageReduction.toBattleSideDamageReduction(): BattleSideDamageReduction =
		BattleSideDamageReduction(
			kind = kind.toEnumValue("state.damageReductions.kind"),
			turnsRemaining = turnsRemaining,
		)

	private fun BattleSandboxStateSnapshot.SpeedModifier.toBattleSideSpeedModifier(): BattleSideSpeedModifier =
		BattleSideSpeedModifier(
			kind = kind.toEnumValue<BattleSideSpeedModifierKind>("state.speedModifiers.kind"),
			multiplier = multiplier,
			turnsRemaining = turnsRemaining,
		)

	private fun BattleSandboxStateSnapshot.EntryHazard.toBattleSideEntryHazard(): BattleSideEntryHazard =
		BattleSideEntryHazard(
			kind = kind.toEnumValue<BattleSideEntryHazardKind>("state.entryHazards.kind"),
			layers = layers,
			maxLayers = maxLayers,
		)

	private fun BattleState.toSandboxResponse(
		resolved: Boolean,
		violations: List<BattleActionViolationResponse>,
		randomTrace: List<BattleRandomTraceEntry>,
		previousEvents: List<BattleSandboxTurnResponse.Event>,
		previousTurns: List<BattleSandboxStateSnapshot.TurnRecord>,
		submittedActions: List<BattleActionRequest>? = null,
	): BattleSandboxTurnResponse {
		val currentEvents = events.map { it.toSandboxEvent() }
		val responseEvents = previousEvents + currentEvents
		val responseRandomTrace = randomTrace.map { it.toSandboxRandomTrace() }
		val ruleHits = ruleHitMapper.summaries(currentEvents, violations, randomTrace)
		val responseTurns = if (resolved && submittedActions != null) {
			previousTurns + BattleSandboxStateSnapshot.TurnRecord(
				turnNumber = turnNumber,
				actions = submittedActions,
				randomTrace = responseRandomTrace,
				events = currentEvents.filter { it.turnNumber == turnNumber },
			)
		} else {
			previousTurns
		}
		return BattleSandboxTurnResponse(
			resolved = resolved,
			turnNumber = turnNumber,
			result = result?.let {
				BattleSandboxTurnResponse.Result(
					winningSideId = it.winningSideId,
					reason = it.reason,
				)
			},
			sides = sides.map { it.toSandboxSide() },
			events = responseEvents,
			violations = violations,
			ruleHits = ruleHits,
			randomTrace = responseRandomTrace,
			state = toSandboxState(responseEvents, responseTurns),
		)
	}

	private fun BattleState.toSandboxState(
		responseEvents: List<BattleSandboxTurnResponse.Event>,
		responseTurns: List<BattleSandboxStateSnapshot.TurnRecord>,
	): BattleSandboxStateSnapshot =
		BattleSandboxStateSnapshot(
			turnNumber = turnNumber,
			result = result?.let {
				BattleSandboxTurnResponse.Result(
					winningSideId = it.winningSideId,
					reason = it.reason,
				)
			},
			environment = environment.toSandboxEnvironment(),
			sides = sides.map { it.toSandboxStateSide() },
			events = responseEvents,
			turns = responseTurns,
		)

	private fun BattleEnvironment.toSandboxEnvironment(): BattleSandboxStateSnapshot.Environment =
		BattleSandboxStateSnapshot.Environment(
			weather = weather.name,
			weatherTurnsRemaining = weatherTurnsRemaining,
			terrain = terrain.name,
			terrainTurnsRemaining = terrainTurnsRemaining,
			fieldSpeedOrderEffect = fieldSpeedOrderEffect?.let {
				BattleSandboxStateSnapshot.FieldSpeedOrderEffect(
					kind = it.kind.name,
					turnsRemaining = it.turnsRemaining,
				)
			},
		)

	private fun BattleSide.toSandboxStateSide(): BattleSandboxStateSnapshot.Side =
		BattleSandboxStateSnapshot.Side(
			sideId = sideId,
			activeActorIds = activeActorIds,
			participants = participants.map { it.toSandboxStateParticipant() },
			damageReductions = damageReductions.map {
				BattleSandboxStateSnapshot.DamageReduction(
					kind = it.kind.name,
					turnsRemaining = it.turnsRemaining,
				)
			},
			speedModifiers = speedModifiers.map {
				BattleSandboxStateSnapshot.SpeedModifier(
					kind = it.kind.name,
					multiplier = it.multiplier,
					turnsRemaining = it.turnsRemaining,
				)
			},
			entryHazards = entryHazards.map {
				BattleSandboxStateSnapshot.EntryHazard(
					kind = it.kind.name,
					layers = it.layers,
					maxLayers = it.maxLayers,
				)
			},
		)

	private fun BattleParticipant.toSandboxStateParticipant(): BattleSandboxStateSnapshot.Participant =
		BattleSandboxStateSnapshot.Participant(
			actorId = actorId,
			currentHp = currentHp,
			elementIds = elementIds.toList(),
			grounded = grounded,
			majorStatus = majorStatus?.name,
			statStages = statStages.mapKeys { it.key.name },
			skillSlots = skillSlots.map {
				BattleSandboxStateSnapshot.SkillSlot(
					skillId = it.skillId,
					remainingPp = it.remainingPp,
				)
			},
			activeSkillActionCount = activeSkillActionCount,
			weightReduction = weightReduction,
			protectionChain = protectionChain,
			fatalDamageEndureSkillId = fatalDamageEndureSkillId,
			badPoisonCounter = badPoisonCounter,
			sleepTurnsRemaining = sleepTurnsRemaining,
			chargingSkillId = chargingSkillId,
			chargingTargetActorId = chargingTargetActorId,
			chargingTurnsRemaining = chargingTurnsRemaining,
			rechargeTurnsRemaining = rechargeTurnsRemaining,
			flinched = flinched,
			confusionTurnsRemaining = confusionTurnsRemaining,
			healBlockTurnsRemaining = healBlockTurnsRemaining,
			tauntTurnsRemaining = tauntTurnsRemaining,
			disabledSkillId = disabledSkillId,
			disabledSkillTurnsRemaining = disabledSkillTurnsRemaining,
			tormented = tormented,
			boundByActorId = boundByActorId,
			bindingTurnsRemaining = bindingTurnsRemaining,
			leechSeedSourceSideId = leechSeedSourceSideId,
			leechSeedSourceActiveIndex = leechSeedSourceActiveIndex,
			lastSuccessfulSkillId = lastSuccessfulSkillId,
			accuracyLockTargetActorId = accuracyLockTargetActorId,
			accuracyLockTurnsRemaining = accuracyLockTurnsRemaining,
			lockedMoveSkillId = lockedMoveSkillId,
			lockedMoveTargetActorId = lockedMoveTargetActorId,
			lockedMoveTurnsRemaining = lockedMoveTurnsRemaining,
			lockedMoveConfusesOnEnd = lockedMoveConfusesOnEnd,
			choiceLockedSkillId = choiceLockedSkillId,
			substituteHp = substituteHp,
		)

	private fun BattleSide.toSandboxSide(): BattleSandboxTurnResponse.Side =
		BattleSandboxTurnResponse.Side(
			sideId = sideId,
			activeActorIds = activeActorIds,
			participants = participants.map { it.toSandboxParticipant(activeActorIds) },
		)

	private fun BattleParticipant.toSandboxParticipant(activeActorIds: List<String>): BattleSandboxTurnParticipant =
		BattleSandboxTurnParticipant {
			actorId = this@toSandboxParticipant.actorId
			creatureId = this@toSandboxParticipant.creatureId
			active = actorId in activeActorIds
			level = this@toSandboxParticipant.level
			currentHp = this@toSandboxParticipant.currentHp
			maxHp = this@toSandboxParticipant.maxHp
			majorStatus = this@toSandboxParticipant.majorStatus?.name
			statStages = this@toSandboxParticipant.statStages.mapKeys { it.key.name }
			skillSlots = this@toSandboxParticipant.skillSlots.map {
				BattleSandboxTurnSkillSlot {
					skillId = it.skillId
					name = it.name
					remainingPp = it.remainingPp
					maxPp = it.maxPp
				}
			}
		}

	private fun BattleEvent.toSandboxEvent(): BattleSandboxTurnResponse.Event {
		val type = this::class.simpleName ?: "BattleEvent"
		// type 是战斗回放、调试和自动化测试依赖的稳定事件编码；typeLabel 是管理页直接展示的中文短名。
		// 两者都从后端事件事实生成，前端就不需要再维护一份容易漂移的事件文案字典。
		return BattleSandboxTurnResponse.Event(
			type = type,
			typeLabel = type.toSandboxEventTypeText(),
			turnNumber = turnNumber,
			message = toSandboxEventMessage(type),
			payload = eventPayload(),
		)
	}

	private fun BattleEvent.toSandboxEventMessage(type: String): String =
		when (this) {
			is BattleEvent.BattleStarted -> "战斗开始，赛制为 $formatCode。"
			is BattleEvent.TurnStarted -> "第 $turnNumber 回合开始。"
			is BattleEvent.SkillUsed -> "$actorId 使用了 $skillName。"
			is BattleEvent.SkillFailed -> "$actorId 的技能 $skillId 失败：${reason.toSkillFailedReasonText()}。"
			is BattleEvent.SkillPpReduced -> "$targetActorId 的技能 $reducedSkillId 减少 $amount 点 PP。"
			is BattleEvent.SideProtectionStarted -> "$sideId 建立了${kind.toSideProtectionKindText()}。"
			is BattleEvent.ProtectionBroken -> "$actorId 破除了 $targetActorId 的${toBrokenProtectionText()}。"
			is BattleEvent.DamageApplied -> "$targetActorId 受到 $amount 点伤害。"
			is BattleEvent.HpAveragedBySkill -> "$actorId 与 $targetActorId 的 HP 被平均为 $averageHp。"
			is BattleEvent.HealingApplied -> "$actorId 回复 $amount 点 HP。"
			is BattleEvent.HeldItemDamageApplied -> "$actorId 因携带道具 $itemId 受到 $amount 点伤害。"
			is BattleEvent.HeldItemTransferred -> "$fromActorId 的携带道具 $itemId 转移给 $toActorId。"
			is BattleEvent.OpponentHeldItemRevealed -> "$actorId 察觉到 $targetActorId 携带道具 $itemId。"
			is BattleEvent.OpponentSkillRevealed -> "$actorId 预知到 $targetActorId 的技能 $skillId。"
			is BattleEvent.ParticipantTransformed -> "$actorId 变身为 $targetActorId 的战斗形态 $copiedCreatureId。"
			is BattleEvent.DangerousOpponentSkillDetected -> "$actorId 察觉到 $targetActorId 的危险技能 $skillId。"
			is BattleEvent.ParticipantFainted -> "$actorId 倒下。"
			is BattleEvent.ParticipantSwitched -> "$sideId 将 $previousActorId 替换为 $nextActorId。"
			is BattleEvent.AccuracyLockStarted -> "$actorId 锁定了 $targetActorId。"
			is BattleEvent.TurnEnded -> "第 $turnNumber 回合结束。"
			is BattleEvent.BattleEnded -> winningSideId?.let { "$it 获胜，原因：$reason。" } ?: "战斗结束，原因：$reason。"
			else -> type.toSandboxEventTypeText()
		}

	private fun String.toSkillFailedReasonText(): String =
		when (this) {
			"target-level-greater-than-user-level" -> "目标等级高于使用者"
			"target-attack-stage-minimum" -> "目标攻击能力阶级已经最低"
			"target-has-no-major-status" -> "目标没有可处理的主要异常状态"
			"user-lacks-removable-element" -> "使用者没有可移除的属性"
			"target-hp-not-greater-than-user-hp" -> "目标当前 HP 不高于使用者"
			"target-behind-substitute" -> "目标正受到替身保护"
			"accuracy-lock-already-active" -> "使用者已经锁定当前目标"
			"target-has-no-last-skill-with-pp" -> "目标没有可扣减 PP 的最近技能"
			"attack-stage-already-maximum" -> "攻击能力阶级已经最高"
			"insufficient-hp-for-max-attack-cost" -> "剩余 HP 不足以支付技能代价"
			"not-first-skill-action-since-entering" -> "不是本次上场后的第一次技能行动"
			"no-pending-skill-action-after-side-protection" -> "本回合没有后续技能行动"
			"target-has-no-pending-skill-action" -> "目标没有待执行的技能行动"
			"target-not-pending-damaging-skill" -> "目标没有准备伤害技能"
			"target-not-pending-priority-damaging-skill" -> "目标没有准备先制伤害技能"
			else -> this
		}

	private fun BattleSideProtectionKind.toSideProtectionKindText(): String =
		when (this) {
			BattleSideProtectionKind.STAT_STAGE_REDUCTION -> "能力下降防护"
			BattleSideProtectionKind.STATUS_CONDITION -> "状态防护"
			BattleSideProtectionKind.MULTI_TARGET_SKILL -> "范围技能防护"
			BattleSideProtectionKind.PRIORITY_SKILL -> "先制技能防护"
		}

	private fun BattleEvent.ProtectionBroken.toBrokenProtectionText(): String =
		buildList {
			if (brokeActorProtection) {
				add("个人保护")
			}
			addAll(brokenSideProtectionKinds.map { it.toSideProtectionKindText() })
		}.joinToString(separator = "、")

	private fun String.toSandboxEventTypeText(): String =
		when (this) {
			"BattleStarted" -> "战斗开始"
			"TurnStarted" -> "回合开始"
			"SkillUsed" -> "使用技能"
			"AccuracyLockStarted" -> "命中锁定"
			"CriticalHitStageBoostStarted" -> "要害等级提升"
			"SkillPpReduced" -> "技能 PP 扣减"
			"SkillMissed" -> "技能未命中"
			"SkillFailed" -> "技能失败"
			"ProtectionStarted" -> "保护开始"
			"ProtectionFailed" -> "保护失败"
			"ProtectionBroken" -> "保护破除"
			"FatalDamageEndureStarted" -> "挺住开始"
			"SkillBlockedByProtection" -> "保护阻挡"
			"SkillBlockedByTerrain" -> "场地阻挡"
			"SkillBlockedByAbility" -> "特性阻挡"
			"SkillBlockedByElement" -> "属性无效"
			"SkillAbsorbedByAbility" -> "特性吸收"
			"ParticipantElementsChanged" -> "属性变化"
			"MultiHitCountDetermined" -> "连击次数"
			"LockedMoveStarted" -> "锁招开始"
			"LockedMoveAdvanced" -> "锁招推进"
			"LockedMoveEnded" -> "锁招结束"
			"SkillPrevented" -> "技能受阻"
			"SkillDisabled" -> "定身"
			"BindingDamageApplied" -> "束缚伤害"
			"LeechSeedPlanted" -> "寄生种子"
			"LeechSeedDamageApplied" -> "寄生伤害"
			"LeechSeedCleared" -> "寄生解除"
			"DamageApplied" -> "造成伤害"
			"StatusApplied" -> "状态施加"
			"StatusApplicationBlocked" -> "状态阻挡"
			"StatusCleared" -> "状态解除"
			"VolatileStatusApplied" -> "临时状态施加"
			"VolatileStatusApplicationBlocked" -> "临时状态阻挡"
			"VolatileStatusCleared" -> "临时状态解除"
			"StatStageChanged" -> "能力变化"
			"StatStageChangeBlocked" -> "能力变化阻挡"
			"WeightReductionChanged" -> "体重变化"
			"StatStageCleared" -> "能力清除"
			"StatStageCopied" -> "能力复制"
			"StatStageSwapped" -> "能力交换"
			"StatStageInverted" -> "能力反转"
			"SideDamageReductionStarted" -> "屏障开始"
			"SideDamageReductionsRemoved" -> "屏障移除"
			"SideProtectionStarted" -> "一侧防护开始"
			"SideProtectionsRemoved" -> "防护移除"
			"SideSpeedModifierStarted" -> "速度修正开始"
			"SideEntryHazardChanged" -> "入场陷阱变化"
			"SideEntryHazardRemoved" -> "入场陷阱移除"
			"EntryHazardDamageApplied" -> "入场伤害"
			"EntryHazardStatusApplied" -> "入场状态"
			"EntryHazardStatusApplicationBlocked" -> "入场状态阻挡"
			"EntryHazardStatStageChanged" -> "入场能力变化"
			"FieldSpeedOrderStarted" -> "场地速度顺序开始"
			"FieldSpeedOrderEnded" -> "场地速度顺序结束"
			"ResidualDamageApplied" -> "回合末伤害"
			"RecoilDamageApplied" -> "反作用伤害"
			"HeldItemDamageApplied" -> "携带道具伤害"
			"HeldItemTransferred" -> "携带道具转移"
			"ConfusionDamageApplied" -> "混乱伤害"
			"HealingApplied" -> "体力回复"
			"LeechSeedHealingApplied" -> "寄生回复"
			"SkillHealingApplied" -> "技能回复"
			"SkillRecoilDamageApplied" -> "技能反作用伤害"
			"SkillSelfSacrificeDamageApplied" -> "自损伤害"
			"HpAveragedBySkill" -> "体力平均"
			"FatalDamageSurvived" -> "濒死保留"
			"DamageReducedByItem" -> "道具减伤"
			"SubstituteStarted" -> "替身开始"
			"SubstituteDamageApplied" -> "替身受伤"
			"SubstituteBroken" -> "替身破坏"
			"SubstituteCleared" -> "替身清除"
			"RechargeStarted" -> "休整开始"
			"SkillChargeStarted" -> "蓄力开始"
			"SkillChargeSkippedByItem" -> "道具跳过蓄力"
			"SkillChargeReleased" -> "蓄力释放"
			"SkillChargeInterrupted" -> "蓄力中断"
			"TerrainHealingApplied" -> "场地回复"
			"WeatherDamageApplied" -> "天气伤害"
			"WeatherHealingApplied" -> "天气回复"
			"WeatherStarted" -> "天气开始"
			"WeatherEnded" -> "天气结束"
			"TerrainStarted" -> "场地开始"
			"TerrainEnded" -> "场地结束"
			"ParticipantFainted" -> "精灵倒下"
			"OpponentHeldItemRevealed" -> "公开对手道具"
			"OpponentSkillRevealed" -> "公开对手技能"
			"ParticipantTransformed" -> "精灵变身"
			"DangerousOpponentSkillDetected" -> "察觉危险技能"
			"ParticipantSwitched" -> "替换精灵"
			"TargetForcedSwitchSelected" -> "强制替换目标"
			"SwitchPrevented" -> "替换受阻"
			"TurnEnded" -> "回合结束"
			"BattleEnded" -> "战斗结束"
			else -> this
		}

	private fun BattleEvent.eventPayload(): Map<String, Any?> =
		this::class.memberProperties
			.filterNot { it.name == "turnNumber" }
			.associate { property -> property.name to property.getter.call(this).toSandboxPayloadValue() }

	private fun Any?.toSandboxPayloadValue(): Any? =
		when (this) {
			null, is String, is Number, is Boolean -> this
			is Enum<*> -> name
			is Iterable<*> -> map { it.toSandboxPayloadValue() }
			is Map<*, *> -> entries.associate { (key, value) -> key.toString() to value.toSandboxPayloadValue() }
			else -> toString()
		}

	private inline fun <reified T : Enum<T>> String.toEnumValue(fieldName: String): T =
		enumValues<T>().firstOrNull { it.name == uppercase(Locale.ROOT) }
			?: invalidValue(fieldName, "$fieldName 不是合法值")

	private fun BattleRandomTraceEntry.toSandboxRandomTrace(): BattleSandboxTurnResponse.RandomTrace =
		BattleSandboxTurnResponse.RandomTrace(
			sequence = sequence,
			bound = bound,
			reason = reason,
			value = value,
		)

	/**
	 * 沙盒专用随机源。
	 *
	 * 使用 JDK `SplittableRandom` 可以让同一个请求在不同机器上稳定复现，又避免调用方必须预先知道本回合会消费
	 * 多少个随机数。真正的公开规则对照测试仍使用严格 trace 随机源，沙盒只承担交互式调试。
	 */
	private class SeededBattleRandom(seed: Long) : BattleRandom {
		private val random = SplittableRandom(seed)

		override fun nextInt(bound: Int, reason: String): Int = random.nextInt(bound)
	}
}
