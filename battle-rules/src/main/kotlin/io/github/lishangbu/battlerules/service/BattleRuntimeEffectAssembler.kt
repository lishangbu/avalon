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
		val abilityPolicies = dataLookup.enabledAbilityPolicies(abilityId)
		validateAbilityPolicies(abilityPolicies, dataLookup.coreElementIds())
		return grounded(abilityPolicies)
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
	 * 未知 policy 直接按资料错误拒绝，而不是被 `mapNotNull` 静默吞掉。`ground-immunity` 是唯一允许返回 null 的
	 * 特性策略，因为它会由 [grounded] 写入成员接地事实，不作为 [BattleAbilityEffect] 暴露给引擎。
	 */
	fun abilityEffects(
		abilityPolicies: List<String>,
		elementIds: Map<String, Long>,
	): List<BattleAbilityEffect> =
		abilityPolicies.mapNotNull { policy ->
			val effect = policy.toBattleAbilityEffect(elementIds)
			when {
				effect != null -> effect
				policy == "ground-immunity" -> null
				else -> invalidValue("effectPolicy", "不支持的特性战斗效果策略: $policy")
			}
		}

	/**
	 * 只校验特性 policy 是否能被当前运行时承载。
	 *
	 * 接地查询只需要 `ground-immunity` 这个旁路事实；如果为了复用 [abilityEffects] 而构造完整效果列表，
	 * 会为天气、属性吸收、伤害修正等策略创建一批随后立即丢弃的对象。这里保留同样的未知策略失败语义，
	 * 但把校验成本限制在“是否支持”的判断上。
	 */
	private fun validateAbilityPolicies(
		abilityPolicies: List<String>,
		elementIds: Map<String, Long>,
	) {
		val unsupported = abilityPolicies.firstOrNull { policy ->
			!policy.isBattleAbilityRuntimePolicySupported(elementIds)
		}
		if (unsupported != null) {
			invalidValue("effectPolicy", "不支持的特性战斗效果策略: $unsupported")
		}
	}

	/**
	 * 将已缓存的道具 policy 转换成结构化效果。
	 *
	 * 道具没有类似接地事实的旁路字段，因此所有启用中的道具 policy 都必须映射成 [BattleItemEffect]。如果这里
	 * 返回空值，战斗引擎就完全不会执行该资料行，所以未知策略必须立即失败。
	 */
	fun itemEffects(
		itemPolicies: List<String>,
		elementIds: Map<String, Long>,
	): List<BattleItemEffect> =
		itemPolicies.map { policy ->
			policy.toBattleItemEffect(elementIds)
				?: invalidValue("effectPolicy", "不支持的道具战斗效果策略: $policy")
		}

	private fun requirePositive(id: Long, field: String) {
		if (id <= 0) {
			invalidValue(field, "$field 必须大于 0")
		}
	}
}
