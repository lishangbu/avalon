package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleSkillSlot
import org.springframework.stereotype.Component

/**
 * 战斗运行时资料读取门面。
 *
 * [BattleRuntimeSnapshotService] 是应用层编排入口，理想情况下不应该直接知道赛制、成员、技能、特性、道具分别由
 * 哪个 SQL/Jimmer 读取器提供。这个门面保持原来的单一注入点，但自身不再承接具体查询；它只把请求按资料族转发给
 * 更小的协作者。
 *
 * 这样做的边界比较明确：
 * - 服务层继续表达“我要运行时资料”，而不是持有一把读取器集合。
 * - 读取器按变更原因拆分，避免一个 700 多行类同时因为赛制、技能、成员公式、特性或道具变化而修改。
 * - 门面不做缓存；单请求缓存仍留在 [BattleRuntimeSnapshotService] 的装配缓存里，因为缓存粒度依赖一次请求的队伍。
 */
@Component
class BattleRuntimeDataLookup(
	private val formatLookup: BattleFormatRuntimeLookup,
	private val coreLookup: BattleCoreRuntimeLookup,
	private val skillLookup: BattleSkillRuntimeLookup,
	private val effectPolicyLookup: BattleEffectPolicyRuntimeLookup,
) {
	fun runtimeSnapshotByFormatCode(formatCode: String, elementIds: Map<String, Long>): BattleRuntimeSnapshot =
		formatLookup.runtimeSnapshotByFormatCode(
			formatCode = formatCode,
			elementIds = elementIds,
			elementChart = coreLookup.elementChart(),
		)

	fun coreElementIds(): Map<String, Long> =
		coreLookup.coreElementIds()

	fun creatureRuntimeProfile(
		creatureId: Long,
		level: Int,
		statConfig: BattleParticipantStatConfig = BattleParticipantStatConfig.DEFAULT,
	): BattleCreatureRuntimeProfile =
		coreLookup.creatureRuntimeProfile(creatureId, level, statConfig)

	fun skillSlotBySkillId(skillId: Long): BattleSkillSlot =
		skillLookup.skillSlotBySkillId(skillId)

	fun enabledAbilityPolicies(abilityId: Long): List<String> =
		effectPolicyLookup.enabledAbilityPolicies(abilityId)

	fun enabledItemPolicies(itemId: Long): List<String> =
		effectPolicyLookup.enabledItemPolicies(itemId)
}
