package io.github.lishangbu.battlerules.service

import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderApplication
import io.github.lishangbu.battleengine.model.BattleFieldSpeedOrderEffect
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleSideConditionApplication
import io.github.lishangbu.battleengine.model.BattleSideConditionTarget
import io.github.lishangbu.battleengine.model.BattleSideDamageReduction
import io.github.lishangbu.battleengine.model.BattleSideEntryHazard
import io.github.lishangbu.battleengine.model.BattleSideEntryHazardApplication
import io.github.lishangbu.battleengine.model.BattleSideProtection
import io.github.lishangbu.battleengine.model.BattleSideProtectionApplication
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifier
import io.github.lishangbu.battleengine.model.BattleSideSpeedModifierApplication
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleStatStageOperation
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.model.BattleTerrain
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.model.BattleWeather
import io.github.lishangbu.battlerules.entity.BattleFieldRule
import io.github.lishangbu.battlerules.entity.BattleSkillChargeSkipWeather
import io.github.lishangbu.battlerules.entity.BattleSkillFieldEffect
import io.github.lishangbu.battlerules.entity.BattleSkillGlobalFieldEffect
import io.github.lishangbu.battlerules.entity.BattleSkillStatStageEffect as BattleSkillStatStageEffectEntity
import io.github.lishangbu.battlerules.entity.BattleSkillStatStageOperation as BattleSkillStatStageOperationEntity
import io.github.lishangbu.battlerules.entity.BattleSkillStatusEffect
import io.github.lishangbu.battlerules.entity.BattleSkillTerrainElementOverride
import io.github.lishangbu.battlerules.entity.BattleSkillTerrainPowerModifier
import io.github.lishangbu.battlerules.entity.BattleSkillWeatherAccuracyOverride
import io.github.lishangbu.battlerules.entity.BattleSkillWeatherElementOverride
import io.github.lishangbu.battlerules.entity.BattleSkillWeatherPowerModifier
import io.github.lishangbu.battlerules.entity.BattleStatusRule
import io.github.lishangbu.battlerules.entity.BattleTerrainRule
import io.github.lishangbu.battlerules.entity.BattleWeatherRule
import io.github.lishangbu.battlerules.entity.effectTiming
import io.github.lishangbu.battlerules.entity.enabled
import io.github.lishangbu.battlerules.entity.id
import io.github.lishangbu.battlerules.entity.skillRuleId
import io.github.lishangbu.battlerules.entity.sortOrder
import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.gamedata.entity.GameElement
import io.github.lishangbu.gamedata.entity.GameStat
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Component

/**
 * 技能规则效果子表运行时读取器。
 *
 * `game_skill` 和 `battle_skill_rule` 主行只描述“这个技能是什么”以及“使用哪类运行时 policy”。真正需要多行维护的
 * 明细，例如天气命中修正、天气属性覆盖、主要状态附加、能力阶级变化、一侧场地状态和全场速度顺序效果，都落在
 * `battle_skill_rule.id` 下面的子表。本类专门读取这些子表，并一次性返回 [BattleSkillRuleEffectRuntimeSnapshot]。
 *
 * 这里不读取技能名称、PP、威力、目标 policy，也不校验 policy 是否被支持；这些仍然属于 [BattleSkillRuntimeLookup]
 * 的主行装配职责。这样分开后，新增一个子表效果只会改动本类和对应快照测试；新增一个主行布尔字段或 policy 校验也
 * 不会让子表查询跟着一起膨胀。
 */
@Component
class BattleSkillRuleEffectRuntimeLookup(
	private val sqlClient: KSqlClient,
) {
	/**
	 * 读取某条技能规则挂载的全部多行效果。
	 *
	 * 返回值按 battle-engine 的强类型模型组织，而不是暴露数据库行。这样上层装配 [BattleSkillRuntimeLookup] 只需要把
	 * 快照字段复制进 `BattleSkillSlot`，不会关心某个效果来自天气表、状态表、能力阶级表还是场地表。
	 */
	fun ruleEffects(ruleId: Long): BattleSkillRuleEffectRuntimeSnapshot {
		val statusEffects = statusEffectRows(ruleId)
		val sideFieldEffects = sideFieldEffectRows(ruleId)
		validateStatusEffectKinds(statusEffects)
		validateSideFieldEffectPolicies(sideFieldEffects)
		return BattleSkillRuleEffectRuntimeSnapshot(
			chargeSkippedByWeathers = chargeSkippedByWeathers(ruleId),
			accuracyOverridesByWeather = weatherAccuracyOverrides(ruleId),
			powerMultipliersByWeather = weatherPowerMultipliers(ruleId),
			groundedPowerMultipliersByTerrain = groundedTerrainPowerMultipliers(ruleId),
			elementOverridesByWeather = weatherElementOverrides(ruleId),
			elementOverridesByTerrain = terrainElementOverrides(ruleId),
			statusApplications = statusApplications(statusEffects),
			volatileStatusApplications = volatileStatusApplications(statusEffects),
			statStageEffects = statStageEffects(ruleId),
			statStageOperations = statStageOperations(ruleId),
			sideConditionApplications = sideConditionApplications(sideFieldEffects),
			sideSpeedModifierApplications = sideSpeedModifierApplications(sideFieldEffects),
			sideEntryHazardApplications = sideEntryHazardApplications(sideFieldEffects),
			sideProtectionApplications = sideProtectionApplications(sideFieldEffects),
			fieldSpeedOrderApplications = fieldSpeedOrderApplications(ruleId),
		)
	}

		private fun weatherAccuracyOverrides(ruleId: Long): Map<BattleWeather, Int?> =
			sqlClient.executeQuery(BattleSkillWeatherAccuracyOverride::class) {
				where(table.skillRuleId eq ruleId)
				where(table.enabled eq true)
				orderBy(table.sortOrder, table.id)
				select(table)
			}.let { overrides ->
				val weatherRules = enabledWeatherRules(overrides.map { override -> override.weatherRuleId })
				overrides.mapNotNull { override ->
					weatherRules[override.weatherRuleId]?.code?.toBattleWeather()?.let { weather ->
						weather to override.accuracyPercent
					}
				}.toMap()
			}

		private fun weatherPowerMultipliers(ruleId: Long): Map<BattleWeather, Double> =
			sqlClient.executeQuery(BattleSkillWeatherPowerModifier::class) {
				where(table.skillRuleId eq ruleId)
				where(table.enabled eq true)
				orderBy(table.sortOrder, table.id)
				select(table)
			}.let { modifiers ->
				val weatherRules = enabledWeatherRules(modifiers.map { modifier -> modifier.weatherRuleId })
				modifiers.mapNotNull { modifier ->
					weatherRules[modifier.weatherRuleId]?.code?.toBattleWeather()?.let { weather ->
						weather to modifier.powerMultiplier
					}
				}.toMap()
			}

		private fun weatherElementOverrides(ruleId: Long): Map<BattleWeather, Long> =
			sqlClient.executeQuery(BattleSkillWeatherElementOverride::class) {
				where(table.skillRuleId eq ruleId)
				where(table.enabled eq true)
				orderBy(table.sortOrder, table.id)
				select(table)
			}.let { overrides ->
				val weatherRules = enabledWeatherRules(overrides.map { override -> override.weatherRuleId })
				val enabledElementIds = enabledElementIds(overrides.map { override -> override.targetElementId })
				overrides.mapNotNull { override ->
					if (override.targetElementId !in enabledElementIds) {
						return@mapNotNull null
					}
					weatherRules[override.weatherRuleId]?.code?.toBattleWeather()?.let { weather ->
						weather to override.targetElementId
					}
				}.toMap()
			}

		private fun groundedTerrainPowerMultipliers(ruleId: Long): Map<BattleTerrain, Double> =
			sqlClient.executeQuery(BattleSkillTerrainPowerModifier::class) {
				where(table.skillRuleId eq ruleId)
				where(table.enabled eq true)
				orderBy(table.sortOrder, table.id)
				select(table)
			}.let { modifiers ->
				val terrainRules = enabledTerrainRules(modifiers.map { modifier -> modifier.terrainRuleId })
				modifiers.mapNotNull { modifier ->
					terrainRules[modifier.terrainRuleId]?.code?.toBattleTerrain()?.let { terrain ->
						terrain to modifier.powerMultiplier
					}
				}.toMap()
			}

		private fun terrainElementOverrides(ruleId: Long): Map<BattleTerrain, Long> =
			sqlClient.executeQuery(BattleSkillTerrainElementOverride::class) {
				where(table.skillRuleId eq ruleId)
				where(table.enabled eq true)
				orderBy(table.sortOrder, table.id)
				select(table)
			}.let { overrides ->
				val terrainRules = enabledTerrainRules(overrides.map { override -> override.terrainRuleId })
				val enabledElementIds = enabledElementIds(overrides.map { override -> override.targetElementId })
				overrides.mapNotNull { override ->
					if (override.targetElementId !in enabledElementIds) {
						return@mapNotNull null
					}
					terrainRules[override.terrainRuleId]?.code?.toBattleTerrain()?.let { terrain ->
						terrain to override.targetElementId
					}
				}.toMap()
			}

		private fun chargeSkippedByWeathers(ruleId: Long): Set<BattleWeather> =
			sqlClient.executeQuery(BattleSkillChargeSkipWeather::class) {
				where(table.skillRuleId eq ruleId)
				where(table.enabled eq true)
				orderBy(table.sortOrder, table.id)
				select(table)
			}.let { skips ->
				val weatherRules = enabledWeatherRules(skips.map { skip -> skip.weatherRuleId })
				skips.mapNotNull { skip -> weatherRules[skip.weatherRuleId]?.code?.toBattleWeather() }.toSet()
			}

	/**
	 * 一次读取技能附加状态效果，再按状态族拆成主要异常和临时状态。
	 *
	 * 数据库中主要异常和临时状态共用 `battle_skill_status_effect`，真正区分来自 `battle_status_rule.status_kind`。
	 * 先把共用行读成 [StatusEffectRuntimeRow]，可以保证目标作用域、概率和排序只解析一次；后续如果状态效果新增
	 * 持续回合或触发时机字段，也只需要在这一个查询里补列。目标作用域必须显式可识别，不能返回 null 后被
	 * `filterNotNull` 吞掉；状态附加行一旦被静默丢弃，实战里会表现成技能命中但异常状态永远不会触发。
	 */
		private fun statusEffectRows(ruleId: Long): List<StatusEffectRuntimeRow> =
			sqlClient.executeQuery(BattleSkillStatusEffect::class) {
				where(table.skillRuleId eq ruleId)
				where(table.enabled eq true)
				where(table.effectTiming eq AFTER_HIT)
				orderBy(table.sortOrder, table.id)
				select(table)
			}.let { effects ->
				val statusRules = sqlClient.findMapByIds(BattleStatusRule::class, effects.map { effect -> effect.statusRuleId })
					.filterValues { rule -> rule.enabled }
				effects.mapNotNull { effect ->
					statusRules[effect.statusRuleId]?.let { rule ->
						StatusEffectRuntimeRow(
							statusCode = rule.code,
							statusKind = rule.statusKind,
							target = effect.targetScope.toBattleEffectTarget("状态效果"),
							chancePercent = effect.chancePercent,
						)
					}
				}
			}

	private fun statusApplications(rows: List<StatusEffectRuntimeRow>): List<BattleStatusApplication> =
		rows.filter { it.statusKind == "MAJOR" }
			.map { row ->
				BattleStatusApplication(
					status = row.statusCode.toBattleMajorStatus(),
					target = row.target,
					chancePercent = row.chancePercent,
				)
			}

	private fun volatileStatusApplications(rows: List<StatusEffectRuntimeRow>): List<BattleVolatileStatusApplication> =
		rows.filter { it.statusKind == "VOLATILE" }
			.map { row ->
				BattleVolatileStatusApplication(
					status = row.statusCode.toBattleVolatileStatus(),
					target = row.target,
					chancePercent = row.chancePercent,
				)
			}

	/**
	 * 校验状态效果族是否能被当前运行时拆分。
	 *
	 * 状态附加子表先读取成 [StatusEffectRuntimeRow]，再按 `statusKind` 拆成主要异常和临时状态两份列表。如果这里不先
	 * 校验，未知状态族会同时避开两个 `filter`，最终表现成启用的状态规则完全没有进入技能槽。生产环境中这比直接
	 * 报错更危险，因为技能表面上仍能装配成功，只有实战时才发现状态不会触发。
	 */
	private fun validateStatusEffectKinds(rows: List<StatusEffectRuntimeRow>) {
		val unsupported = rows.firstOrNull { row -> row.statusKind != "MAJOR" && row.statusKind != "VOLATILE" }
		if (unsupported != null) {
			invalidValue("statusKind", "不支持的状态效果类型: ${unsupported.statusKind}")
		}
	}

		private fun statStageEffects(ruleId: Long): List<BattleStatStageEffect> =
			sqlClient.executeQuery(BattleSkillStatStageEffectEntity::class) {
				where(table.skillRuleId eq ruleId)
				where(table.enabled eq true)
				where(table.effectTiming eq AFTER_HIT)
				orderBy(table.sortOrder, table.id)
				select(table)
			}.let { effects ->
				val stats = sqlClient.findMapByIds(GameStat::class, effects.map { effect -> effect.statId })
				effects.mapNotNull { effect ->
					stats[effect.statId]?.let { stat ->
						BattleStatStageEffect(
							stat = stat.code.toBattleStat(),
							target = effect.targetScope.toBattleEffectTarget("能力阶级效果"),
							stageDelta = effect.stageDelta,
							chancePercent = effect.chancePercent,
						)
					}
				}
			}

		private fun statStageOperations(ruleId: Long): List<BattleStatStageOperation> =
			sqlClient.executeQuery(BattleSkillStatStageOperationEntity::class) {
				where(table.skillRuleId eq ruleId)
				where(table.enabled eq true)
				where(table.effectTiming eq AFTER_HIT)
				orderBy(table.sortOrder, table.id)
				select(table)
			}.let { operations ->
				val stats = sqlClient.findMapByIds(GameStat::class, operations.map { operation -> operation.statId })
				operations.mapNotNull { operation ->
					stats[operation.statId]?.let { stat ->
						BattleStatStageOperation(
							kind = operation.operationKind.toBattleStatStageOperationKind(),
							stat = stat.code.toBattleStat(),
							target = operation.targetScope.toBattleStatStageOperationTarget(),
							source = operation.sourceScope?.toBattleStatStageOperationTarget(),
							chancePercent = operation.chancePercent,
						)
					}
				}
			}

	/**
	 * 一次读取一侧场地效果，再按 effect_policy 映射成屏障、速度修正或入场陷阱。
	 *
	 * `battle_skill_field_effect` 同时承载三类运行时效果；它们的过滤条件、排序条件和天气前置条件完全一致，差异只在
	 * `battle_field_rule.effect_policy` 最终能映射成哪种引擎模型。保留三段几乎相同的查询会让新增字段时出现三处
	 * 同步点，也会让每个技能规则多做两次无意义查询。因此这里先冻结成行对象，再用三个窄映射函数拆回
	 * `BattleSkillSlot` 需要的列表。
	 */
		private fun sideFieldEffectRows(ruleId: Long): List<SideFieldEffectRuntimeRow> =
			sqlClient.executeQuery(BattleSkillFieldEffect::class) {
				where(table.skillRuleId eq ruleId)
				where(table.enabled eq true)
				where(table.effectTiming eq AFTER_HIT)
				orderBy(table.sortOrder, table.id)
				select(table)
			}.let { effects ->
				val fieldRules = sqlClient.findMapByIds(BattleFieldRule::class, effects.map { effect -> effect.fieldRuleId })
					.filterValues { rule -> rule.enabled && rule.effectScope == SIDE_EFFECT_SCOPE }
				val weatherRules = weatherRules(effects.mapNotNull { effect -> effect.requiredWeatherRuleId })
				effects.mapNotNull { effect ->
					fieldRules[effect.fieldRuleId]?.let { fieldRule ->
						SideFieldEffectRuntimeRow(
							effectPolicy = fieldRule.effectPolicy,
							minTurns = fieldRule.minTurns,
							maxLayers = fieldRule.maxLayers,
							targetSide = effect.targetSide.toBattleSideConditionTarget(),
							chancePercent = effect.chancePercent,
							requiredWeather = effect.requiredWeatherRuleId
								?.let(weatherRules::get)
								?.code
								?.toBattleWeather(),
						)
					}
				}
			}

	private fun sideConditionApplications(rows: List<SideFieldEffectRuntimeRow>): List<BattleSideConditionApplication> =
		rows.mapNotNull { row ->
			val reductionKind = row.effectPolicy.toBattleSideDamageReductionKind() ?: return@mapNotNull null
			BattleSideConditionApplication(
				targetSide = row.targetSide,
				damageReduction = BattleSideDamageReduction(
					kind = reductionKind,
					turnsRemaining = row.minTurns,
				),
				chancePercent = row.chancePercent,
				requiredWeather = row.requiredWeather,
			)
		}

	private fun sideSpeedModifierApplications(rows: List<SideFieldEffectRuntimeRow>): List<BattleSideSpeedModifierApplication> =
		rows.mapNotNull { row ->
			val modifierKind = row.effectPolicy.toBattleSideSpeedModifierKind() ?: return@mapNotNull null
			BattleSideSpeedModifierApplication(
				targetSide = row.targetSide,
				speedModifier = BattleSideSpeedModifier(
					kind = modifierKind,
					turnsRemaining = row.minTurns,
				),
				chancePercent = row.chancePercent,
				requiredWeather = row.requiredWeather,
			)
		}

	private fun sideEntryHazardApplications(rows: List<SideFieldEffectRuntimeRow>): List<BattleSideEntryHazardApplication> =
		rows.mapNotNull { row ->
			val hazardKind = row.effectPolicy.toBattleSideEntryHazardKind() ?: return@mapNotNull null
			BattleSideEntryHazardApplication(
				targetSide = row.targetSide,
				hazard = BattleSideEntryHazard(
					kind = hazardKind,
					maxLayers = row.maxLayers ?: hazardKind.defaultMaxLayers,
				),
				chancePercent = row.chancePercent,
				requiredWeather = row.requiredWeather,
			)
		}

	private fun sideProtectionApplications(rows: List<SideFieldEffectRuntimeRow>): List<BattleSideProtectionApplication> =
		rows.mapNotNull { row ->
			val protectionKind = row.effectPolicy.toBattleSideProtectionKind() ?: return@mapNotNull null
			BattleSideProtectionApplication(
				targetSide = row.targetSide,
				protection = BattleSideProtection(
					kind = protectionKind,
					turnsRemaining = row.minTurns,
				),
				chancePercent = row.chancePercent,
				requiredWeather = row.requiredWeather,
			)
		}

	/**
	 * 校验一侧场地效果 policy 至少能被某一种一侧效果模型承载。
	 *
	 * 同一批 `battle_skill_field_effect` 行会被拆成屏障、速度修正和入场陷阱三个列表，所以单个列表里的 `mapNotNull`
	 * 不能把未命中的 policy 直接视为错误：顺风行对于屏障列表本来就应该返回 null。这里在拆分前先做一次总分类，
	 * 只有当一个 policy 既不是屏障、也不是速度修正、也不是入场陷阱时才失败。这样保留三类列表的简单实现，
	 * 同时避免启用中的资料因为拼写错误或 mapper 漏补，被生产运行时静默装配成“技能没有任何一侧场地效果”。
	 */
	private fun validateSideFieldEffectPolicies(rows: List<SideFieldEffectRuntimeRow>) {
		val unsupported = rows.firstOrNull { row ->
			row.effectPolicy.toBattleSideDamageReductionKind() == null &&
				row.effectPolicy.toBattleSideSpeedModifierKind() == null &&
				row.effectPolicy.toBattleSideEntryHazardKind() == null &&
				row.effectPolicy.toBattleSideProtectionKind() == null
		}
		if (unsupported != null) {
			invalidValue("effectPolicy", "不支持的一侧场地效果策略: ${unsupported.effectPolicy}")
		}
	}

		private fun fieldSpeedOrderApplications(ruleId: Long): List<BattleFieldSpeedOrderApplication> =
			sqlClient.executeQuery(BattleSkillGlobalFieldEffect::class) {
				where(table.skillRuleId eq ruleId)
				where(table.enabled eq true)
				where(table.effectTiming eq AFTER_HIT)
				orderBy(table.sortOrder, table.id)
				select(table)
			}.let { effects ->
				val fieldRules = sqlClient.findMapByIds(BattleFieldRule::class, effects.map { effect -> effect.fieldRuleId })
					.filterValues { rule -> rule.enabled && rule.effectScope == FIELD_EFFECT_SCOPE }
				val weatherRules = weatherRules(effects.mapNotNull { effect -> effect.requiredWeatherRuleId })
				effects.mapNotNull { effect ->
					fieldRules[effect.fieldRuleId]?.let { fieldRule ->
						val speedOrderKind = fieldRule.effectPolicy.toBattleFieldSpeedOrderKind()
							?: invalidValue(
								"effectPolicy",
								"不支持的全场速度顺序效果策略: ${fieldRule.effectPolicy}",
							)
						BattleFieldSpeedOrderApplication(
							speedOrderEffect = BattleFieldSpeedOrderEffect(
								kind = speedOrderKind,
								turnsRemaining = fieldRule.minTurns,
							),
							chancePercent = effect.chancePercent,
							requiredWeather = effect.requiredWeatherRuleId
								?.let(weatherRules::get)
								?.code
								?.toBattleWeather(),
						)
					}
				}
			}

	/**
	 * 解析技能效果子表中的目标作用域。
	 *
	 * `BattleEffectTarget` 只允许“使用者”和“当前实际目标”两种运行时语义；范围技能已经在引擎命中阶段逐目标展开，
	 * 因此资料中的 ALL_OPPONENTS 会在 mapper 中压成 TARGET。除这几种已知值以外，任何目标作用域都代表资料
	 * 或后台写入错误，必须立即失败。这里不让调用方拿到 null，是为了防止查询结果被 `filterNotNull` 继续静默丢弃。
	 */
		private fun String.toBattleEffectTarget(context: String): BattleEffectTarget =
			toBattleEffectTarget()
				?: invalidValue("targetScope", "不支持的${context}目标作用域: $this")

		private fun enabledWeatherRules(ids: Iterable<Long>): Map<Long, BattleWeatherRule> =
			weatherRules(ids).filterValues { rule -> rule.enabled }

		private fun weatherRules(ids: Iterable<Long>): Map<Long, BattleWeatherRule> =
			sqlClient.findMapByIds(BattleWeatherRule::class, ids)

		private fun enabledTerrainRules(ids: Iterable<Long>): Map<Long, BattleTerrainRule> =
			sqlClient.findMapByIds(BattleTerrainRule::class, ids).filterValues { rule -> rule.enabled }

		private fun enabledElementIds(ids: Iterable<Long>): Set<Long> =
			sqlClient.findMapByIds(GameElement::class, ids)
				.filterValues { element -> element.enabled == true }
				.keys

		companion object {
			private const val AFTER_HIT = "AFTER_HIT"
			private const val SIDE_EFFECT_SCOPE = "SIDE"
			private const val FIELD_EFFECT_SCOPE = "FIELD"
		}
	}
