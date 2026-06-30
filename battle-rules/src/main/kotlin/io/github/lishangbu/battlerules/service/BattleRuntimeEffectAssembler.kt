package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleAbilityEffect
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.common.web.invalidValue
import org.springframework.stereotype.Component

/**
 * 战斗运行时效果装配器。
 *
 * battle-rules 中的特性、道具规则以资料侧 policy code 表达，battle-engine 只接受稳定的结构化效果模型。
 * 本组件专门承担这层翻译，避免入口服务和初始状态装配器分别解析同一批字符串。这样后续新增一种特性或道具效果时，
 * 只需要在 [BattleRuntimePolicyMapper] 中补映射，并通过这里统一进入：
 * - 调试/管理接口查询单个特性、道具效果。
 * - 队伍启动快照装配成员身上的特性、道具效果。
 *
 * `ground-immunity` 比较特殊：它不是伤害、状态或回合 hook，而是成员快照上的“是否接地”事实。
 * 因此这里把它装配成布尔值，继续让纯引擎只读取 [io.github.lishangbu.battleengine.model.BattleParticipant.grounded]，
 * 不把数据库 policy 字符串泄漏到战斗结算流程里。
 */
@Component
class BattleRuntimeEffectAssembler(
	private val dataLookup: BattleRuntimeDataLookup,
) {
	/**
	 * 按基础特性 ID 读取启用中的 policy，并转换成引擎可直接消费的特性效果。
	 *
	 * 对外调试接口走这条路径；队伍装配若已经批量缓存了 policy，则使用 [abilityEffects] 避免重复查库。
	 */
	fun abilityEffectsByAbilityId(abilityId: Long?): List<BattleAbilityEffect> {
		if (abilityId == null) {
			return emptyList()
		}
		requirePositive(abilityId, field = "abilityId")
		return abilityEffects(
			abilityPolicies = dataLookup.enabledAbilityPolicies(abilityId),
			elementIds = dataLookup.coreElementIds(),
		)
	}

	/**
	 * 按基础特性 ID 判断成员是否接地。
	 *
	 * 空特性视为接地；非法 ID 仍在应用层报错，避免让“没有特性”和“传错特性”混成同一种状态。
	 */
	fun groundedByAbilityId(abilityId: Long?): Boolean {
		if (abilityId == null) {
			return true
		}
		requirePositive(abilityId, field = "abilityId")
		return grounded(dataLookup.enabledAbilityPolicies(abilityId))
	}

	/**
	 * 按基础道具 ID 读取启用中的 policy，并转换成引擎可直接消费的道具效果。
	 *
	 * 道具不存在或未配置可识别 policy 时返回空列表；这代表“没有结构化战斗效果”，不是运行时 fallback。
	 */
	fun itemEffectsByItemId(itemId: Long?): List<BattleItemEffect> {
		if (itemId == null) {
			return emptyList()
		}
		requirePositive(itemId, field = "itemId")
		return itemEffects(
			itemPolicies = dataLookup.enabledItemPolicies(itemId),
			elementIds = dataLookup.coreElementIds(),
		)
	}

	/**
	 * 将已缓存的特性 policy 转换成接地事实。
	 *
	 * 初始状态装配会在单次请求中缓存 policy；这里保持纯函数形态，让缓存命中后不用再回数据库。
	 */
	fun grounded(abilityPolicies: List<String>): Boolean =
		"ground-immunity" !in abilityPolicies

	/**
	 * 将已缓存的特性 policy 转换成结构化效果。
	 *
	 * 未被当前引擎模型承载的 policy 会被 [toBattleAbilityEffect] 明确丢弃，含义是“资料已维护但引擎尚未接入”。
	 */
	fun abilityEffects(
		abilityPolicies: List<String>,
		elementIds: Map<String, Long>,
	): List<BattleAbilityEffect> =
		abilityPolicies.mapNotNull { it.toBattleAbilityEffect(elementIds) }

	/**
	 * 将已缓存的道具 policy 转换成结构化效果。
	 *
	 * 与特性一样，这里只输出 battle-engine 有稳定模型的效果，避免引擎运行时解析资料库字符串。
	 */
	fun itemEffects(
		itemPolicies: List<String>,
		elementIds: Map<String, Long>,
	): List<BattleItemEffect> =
		itemPolicies.mapNotNull { it.toBattleItemEffect(elementIds) }

	private fun requirePositive(id: Long, field: String) {
		if (id <= 0) {
			invalidValue(field, "$field 必须大于 0")
		}
	}
}
