package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleFixedDamage
import io.github.lishangbu.battleengine.model.BattleItemEffect
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageOperation
import io.github.lishangbu.battleengine.model.BattleStatStageOperationKind
import io.github.lishangbu.battleengine.model.BattleStatStageOperationTarget
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * 验证现代主系列战斗规则第一版收尾阶段的 12 条公开对照边界。
 *
 * 场景类型：命中前目标有效性、防护穿透、能力阶级特殊操作、状态概率失败、蓄力跳过道具、免死道具和治愈道具。
 * 参考来源类型：公开成熟对战引擎行动结算、公开技能/道具资料和公开状态规则说明。该批次用于把最终账本从
 * 300/312 推进到 312/312，因此每个测试都固定一个此前没有单独登记的可复用规则行为。
 * 验证重点：保护只拦截声明受保护影响的技能；目标在行动前失效时不消耗 PP；全场能力阶级操作只处理当前上场；
 * 0% 效果不消费随机；复制、交换和取反只改写对应能力项；一次性道具的消费时机必须早于后续同道具效果。
 */
class BattleFinalRuleBoundaryPublicReferenceTests {
	private val engine = BattleEngine()

	@Test
	fun `unprotected damaging skill bypasses protection and still deals damage`() {
		val scenario = scenario(
			name = "unprotected-damaging-skill-bypasses-protection-and-still-deals-damage",
			inputSummary = "目标本回合先成功建立保护屏障，较慢对手随后使用声明不受保护影响的伤害技能。",
			expectedSummary = "保护屏障不会阻挡该技能；技能继续消费要害和伤害随机，并对目标本体造成伤害。",
		)
		val bypassSkill = damagingSkill(skillId = 301, name = "破防测试", affectedByProtect = false)
		val random = ScriptedBattleRandom(listOf(1, 15))
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("protector", speed = 100, skill = protectionSkill()),
					second = participant("attacker", speed = 50, skill = bypassSkill),
				),
			),
			listOf(
				BattleAction.UseSkill("protector", skillId = 2, targetActorId = "attacker"),
				BattleAction.UseSkill("attacker", skillId = 301, targetActorId = "protector"),
			),
			random,
		)

		scenario.assertNamed("unprotected-damaging-skill-bypasses-protection-and-still-deals-damage")
		assertEquals(72, resolved.participant("protector")?.currentHp)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>())
		assertEquals(listOf("critical hit for 301", "damage random for 301"), random.consumedReasons())
	}

	@Test
	fun `selected target fainted before later action cancels skill use`() {
		val scenario = scenario(
			name = "selected-target-fainted-before-later-action-cancels-skill-use",
			inputSummary = "双打中较快伙伴先击倒一名对手，较慢伙伴原本也把单体技能指向同一上场目标。",
			expectedSummary = "较慢伙伴行动时目标已经无法战斗，技能不宣告、不消耗 PP，也不产生命中或伤害事件。",
		)
		val finisher = damagingSkill(
			skillId = 302,
			name = "收尾测试",
			power = null,
			fixedDamage = BattleFixedDamage.FixedAmount(120),
		)
		val lateSkill = damagingSkill(skillId = 303, name = "迟到目标测试")
		val resolved = engine.resolveTurn(
			engine.start(
				doubleInitialState(
					firstA = participant("finisher", speed = 100, skill = finisher),
					firstB = participant("late-user", speed = 90, skill = lateSkill),
					secondA = participant("target", speed = 80, currentHp = 50),
					secondB = participant("other-opponent", speed = 70),
				),
			),
			listOf(
				BattleAction.UseSkill("finisher", skillId = 302, targetActorId = "target"),
				BattleAction.UseSkill("late-user", skillId = 303, targetActorId = "target"),
			),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("selected-target-fainted-before-later-action-cancels-skill-use")
		assertEquals(0, resolved.participant("target")?.currentHp)
		assertEquals(listOf("finisher"), resolved.events.filterIsInstance<BattleEvent.SkillUsed>().map { it.actorId })
		assertEquals(35, resolved.participant("late-user")?.skillSlot(303)?.remainingPp)
	}

	@Test
	fun `all active stat clear skips bench participants`() {
		val scenario = scenario(
			name = "all-active-stat-clear-skips-bench-participants",
			inputSummary = "全场清除能力阶级效果发动时，双方上场成员和一名后备成员都拥有攻击阶级变化。",
			expectedSummary = "技能只清除当前上场且仍可战斗成员的攻击阶级，后备成员的阶级快照保持不变。",
		)
		val clearSkill = statOperationSkill(
			skillId = 304,
			operation = BattleStatStageOperation(
				kind = BattleStatStageOperationKind.CLEAR,
				stat = BattleStat.ATTACK,
				target = BattleStatStageOperationTarget.ALL_ACTIVE,
				chancePercent = 100,
			),
		)
		val user = participant("clear-user", speed = 100, skill = clearSkill)
			.copy(statStages = mapOf(BattleStat.ATTACK to 3))
		val target = participant("target", speed = 50)
			.copy(statStages = mapOf(BattleStat.ATTACK to -2))
		val bench = participant("bench", speed = 40)
			.copy(statStages = mapOf(BattleStat.ATTACK to 5))

		val resolved = engine.resolveTurn(
			engine.start(initialState(first = user, second = target, firstBench = listOf(bench))),
			listOf(BattleAction.UseSkill("clear-user", skillId = 304, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("all-active-stat-clear-skips-bench-participants")
		assertEquals(0, resolved.participant("clear-user")?.statStage(BattleStat.ATTACK))
		assertEquals(0, resolved.participant("target")?.statStage(BattleStat.ATTACK))
		assertEquals(5, resolved.participant("bench")?.statStage(BattleStat.ATTACK))
		assertEquals(2, resolved.events.filterIsInstance<BattleEvent.StatStageCleared>().size)
	}

	@Test
	fun `zero chance major status effect consumes no random and applies nothing`() {
		val scenario = scenario(
			name = "zero-chance-major-status-effect-consumes-no-random-and-applies-nothing",
			inputSummary = "技能命中后声明 0% 附加灼伤。",
			expectedSummary = "0% 效果稳定失败且不消费概率随机；目标不会获得主要异常状态。",
		)
		val statusSkill = damagingSkill(
			skillId = 305,
			name = "零概率灼伤测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			statusApplications = listOf(
				BattleStatusApplication(
					status = BattleMajorStatus.BURN,
					target = BattleEffectTarget.TARGET,
					chancePercent = 0,
				),
			),
		)
		val random = ScriptedBattleRandom(emptyList())
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("status-user", speed = 100, skill = statusSkill),
					second = participant("target", speed = 50),
				),
			),
			listOf(BattleAction.UseSkill("status-user", skillId = 305, targetActorId = "target")),
			random,
		)

		scenario.assertNamed("zero-chance-major-status-effect-consumes-no-random-and-applies-nothing")
		assertNull(resolved.participant("target")?.majorStatus)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatusApplied>())
		assertEquals(emptyList(), random.consumedReasons())
	}

	@Test
	fun `zero chance volatile effect skips duration random`() {
		val scenario = scenario(
			name = "zero-chance-volatile-effect-skips-duration-random",
			inputSummary = "技能命中后声明 0% 附加混乱，混乱成功时本应消费持续时间随机。",
			expectedSummary = "0% 临时状态效果不消费概率随机，也不会继续消费混乱持续时间随机。",
		)
		val confusionSkill = damagingSkill(
			skillId = 306,
			name = "零概率混乱测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			volatileStatusApplications = listOf(
				BattleVolatileStatusApplication(
					status = BattleVolatileStatus.CONFUSION,
					target = BattleEffectTarget.TARGET,
					chancePercent = 0,
				),
			),
		)
		val random = ScriptedBattleRandom(emptyList())
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("volatile-user", speed = 100, skill = confusionSkill),
					second = participant("target", speed = 50),
				),
			),
			listOf(BattleAction.UseSkill("volatile-user", skillId = 306, targetActorId = "target")),
			random,
		)

		scenario.assertNamed("zero-chance-volatile-effect-skips-duration-random")
		assertEquals(0, resolved.participant("target")?.confusionTurnsRemaining)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplied>())
		assertEquals(emptyList(), random.consumedReasons())
	}

	@Test
	fun `copy operation changes only declared stat`() {
		val scenario = scenario(
			name = "copy-operation-changes-only-declared-stat",
			inputSummary = "使用者复制目标速度阶级；双方其它能力阶级也已有变化。",
			expectedSummary = "复制操作只写入声明的速度阶级，不读取或覆盖攻击等其它能力阶级。",
		)
		val copySkill = statOperationSkill(
			skillId = 307,
			operation = BattleStatStageOperation(
				kind = BattleStatStageOperationKind.COPY,
				stat = BattleStat.SPEED,
				target = BattleStatStageOperationTarget.USER,
				source = BattleStatStageOperationTarget.TARGET,
				chancePercent = 100,
			),
		)
		val user = participant("copy-user", speed = 100, skill = copySkill)
			.copy(statStages = mapOf(BattleStat.ATTACK to 2))
		val target = participant("source", speed = 50)
			.copy(statStages = mapOf(BattleStat.SPEED to 4, BattleStat.ATTACK to -3))

		val resolved = engine.resolveTurn(
			engine.start(initialState(first = user, second = target)),
			listOf(BattleAction.UseSkill("copy-user", skillId = 307, targetActorId = "source")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("copy-operation-changes-only-declared-stat")
		assertEquals(4, resolved.participant("copy-user")?.statStage(BattleStat.SPEED))
		assertEquals(2, resolved.participant("copy-user")?.statStage(BattleStat.ATTACK))
		assertEquals(-3, resolved.participant("source")?.statStage(BattleStat.ATTACK))
		assertEquals(BattleStat.SPEED, resolved.events.filterIsInstance<BattleEvent.StatStageCopied>().single().stat)
	}

	@Test
	fun `swap operation writes both participants in one effect`() {
		val scenario = scenario(
			name = "swap-operation-writes-both-participants-in-one-effect",
			inputSummary = "使用者与目标交换攻击阶级，交换前使用者为 +2，目标为 -3。",
			expectedSummary = "同一次交换效果同时写回双方阶级；使用者变为 -3，目标变为 +2。",
		)
		val swapSkill = statOperationSkill(
			skillId = 308,
			operation = BattleStatStageOperation(
				kind = BattleStatStageOperationKind.SWAP,
				stat = BattleStat.ATTACK,
				target = BattleStatStageOperationTarget.TARGET,
				source = BattleStatStageOperationTarget.USER,
				chancePercent = 100,
			),
		)
		val user = participant("swap-user", speed = 100, skill = swapSkill)
			.copy(statStages = mapOf(BattleStat.ATTACK to 2))
		val target = participant("target", speed = 50)
			.copy(statStages = mapOf(BattleStat.ATTACK to -3))

		val resolved = engine.resolveTurn(
			engine.start(initialState(first = user, second = target)),
			listOf(BattleAction.UseSkill("swap-user", skillId = 308, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("swap-operation-writes-both-participants-in-one-effect")
		assertEquals(-3, resolved.participant("swap-user")?.statStage(BattleStat.ATTACK))
		assertEquals(2, resolved.participant("target")?.statStage(BattleStat.ATTACK))
		val event = resolved.events.filterIsInstance<BattleEvent.StatStageSwapped>().single()
		assertEquals(2, event.firstCurrentStage)
		assertEquals(-3, event.secondCurrentStage)
	}

	@Test
	fun `invert operation turns negative stage positive`() {
		val scenario = scenario(
			name = "invert-operation-turns-negative-stage-positive",
			inputSummary = "目标防御阶级为 -4，使用者命中后执行取反能力阶级操作。",
			expectedSummary = "取反操作按当前阶级写回相反数，目标防御从 -4 变为 +4。",
		)
		val invertSkill = statOperationSkill(
			skillId = 309,
			operation = BattleStatStageOperation(
				kind = BattleStatStageOperationKind.INVERT,
				stat = BattleStat.DEFENSE,
				target = BattleStatStageOperationTarget.TARGET,
				chancePercent = 100,
			),
		)
		val target = participant("target", speed = 50)
			.copy(statStages = mapOf(BattleStat.DEFENSE to -4))

		val resolved = engine.resolveTurn(
			engine.start(initialState(first = participant("invert-user", speed = 100, skill = invertSkill), second = target)),
			listOf(BattleAction.UseSkill("invert-user", skillId = 309, targetActorId = "target")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("invert-operation-turns-negative-stage-positive")
		assertEquals(4, resolved.participant("target")?.statStage(BattleStat.DEFENSE))
		val event = resolved.events.filterIsInstance<BattleEvent.StatStageInverted>().single()
		assertEquals(-4, event.previousStage)
		assertEquals(4, event.currentStage)
	}

	@Test
	fun `charge skip item is consumed before protection blocks released skill`() {
		val scenario = scenario(
			name = "charge-skip-item-is-consumed-before-protection-blocks-released-skill",
			inputSummary = "携带一次性蓄力跳过道具的成员使用蓄力技能，同回合目标先成功保护。",
			expectedSummary = "道具在技能宣告后立即消费并让技能同回合释放；随后释放出的技能仍会被保护阻挡。",
		)
		val chargeSkill = damagingSkill(skillId = 310, name = "蓄力道具测试", chargesBeforeUse = true)
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("protector", speed = 100, skill = protectionSkill()),
					second = participant(
						"charger",
						speed = 50,
						skill = chargeSkill,
						itemId = 910,
						itemEffects = listOf(BattleItemEffect.ChargeSkipOnce()),
					),
				),
			),
			listOf(
				BattleAction.UseSkill("protector", skillId = 2, targetActorId = "charger"),
				BattleAction.UseSkill("charger", skillId = 310, targetActorId = "protector"),
			),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("charge-skip-item-is-consumed-before-protection-blocks-released-skill")
		assertNull(resolved.participant("charger")?.itemId)
		assertEquals(true, resolved.events.filterIsInstance<BattleEvent.SkillChargeSkippedByItem>().single().consumed)
		assertEquals("protector", resolved.events.filterIsInstance<BattleEvent.SkillBlockedByProtection>().single().targetActorId)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.DamageApplied>())
	}

	@Test
	fun `fatal survival item consumption prevents low hp heal on same hit`() {
		val scenario = scenario(
			name = "fatal-survival-item-consumption-prevents-low-hp-heal-on-same-hit",
			inputSummary = "目标满 HP 携带同一道具上的免死效果和低体力回复效果，受到足以击倒的直接伤害。",
			expectedSummary = "免死效果先触发并消费携带道具，目标保留 1 HP；同一道具的低体力回复不会在同一击继续触发。",
		)
		val fatalSkill = damagingSkill(
			skillId = 311,
			name = "免死道具测试",
			power = null,
			fixedDamage = BattleFixedDamage.FixedAmount(200),
		)
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("attacker", speed = 100, skill = fatalSkill),
					second = participant(
						"holder",
						speed = 50,
						itemId = 911,
						itemEffects = listOf(
							BattleItemEffect.SurviveFatalDamageAtFullHp(),
							BattleItemEffect.LowHpHeal(fixedHealAmount = 20),
						),
					),
				),
			),
			listOf(BattleAction.UseSkill("attacker", skillId = 311, targetActorId = "holder")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("fatal-survival-item-consumption-prevents-low-hp-heal-on-same-hit")
		assertEquals(1, resolved.participant("holder")?.currentHp)
		assertNull(resolved.participant("holder")?.itemId)
		assertEquals(true, resolved.events.filterIsInstance<BattleEvent.FatalDamageSurvived>().single().consumed)
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.HealingApplied>())
	}

	@Test
	fun `non consumable major status cure keeps held item`() {
		val scenario = scenario(
			name = "non-consumable-major-status-cure-keeps-held-item",
			inputSummary = "目标获得灼伤后触发配置为不消费的主要异常状态治愈道具效果。",
			expectedSummary = "灼伤先成为事实再被清除；由于效果声明不消费，目标仍保留携带道具。",
		)
		val burnSkill = damagingSkill(
			skillId = 312,
			name = "不消费治愈测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			statusApplications = listOf(
				BattleStatusApplication(
					status = BattleMajorStatus.BURN,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("status-user", speed = 100, skill = burnSkill),
					second = participant(
						"holder",
						speed = 50,
						itemId = 912,
						itemEffects = listOf(
							BattleItemEffect.MajorStatusCure(
								statuses = setOf(BattleMajorStatus.BURN),
								consumesItem = false,
							),
						),
					),
				),
			),
			listOf(BattleAction.UseSkill("status-user", skillId = 312, targetActorId = "holder")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("non-consumable-major-status-cure-keeps-held-item")
		assertNull(resolved.participant("holder")?.majorStatus)
		assertEquals(912, resolved.participant("holder")?.itemId)
		assertEquals(1, resolved.events.filterIsInstance<BattleEvent.StatusApplied>().size)
		assertEquals(1, resolved.events.filterIsInstance<BattleEvent.StatusCleared>().size)
	}

	@Test
	fun `non consumable volatile status cure keeps held item`() {
		val scenario = scenario(
			name = "non-consumable-volatile-status-cure-keeps-held-item",
			inputSummary = "目标获得混乱后触发配置为不消费的临时状态治愈道具效果。",
			expectedSummary = "混乱先消费持续时间随机并写入运行态，再被治愈效果清除；目标仍保留携带道具。",
		)
		val confusionSkill = damagingSkill(
			skillId = 313,
			name = "不消费混乱治愈测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			volatileStatusApplications = listOf(
				BattleVolatileStatusApplication(
					status = BattleVolatileStatus.CONFUSION,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)
		val random = ScriptedBattleRandom(listOf(1))
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("volatile-user", speed = 100, skill = confusionSkill),
					second = participant(
						"holder",
						speed = 50,
						itemId = 913,
						itemEffects = listOf(
							BattleItemEffect.VolatileStatusCure(
								statuses = setOf(BattleVolatileStatus.CONFUSION),
								consumesItem = false,
							),
						),
					),
				),
			),
			listOf(BattleAction.UseSkill("volatile-user", skillId = 313, targetActorId = "holder")),
			random,
		)

		scenario.assertNamed("non-consumable-volatile-status-cure-keeps-held-item")
		assertEquals(0, resolved.participant("holder")?.confusionTurnsRemaining)
		assertEquals(913, resolved.participant("holder")?.itemId)
		assertEquals(listOf("confusion duration for 313"), random.consumedReasons())
		assertEquals(1, resolved.events.filterIsInstance<BattleEvent.VolatileStatusApplied>().size)
		assertEquals(1, resolved.events.filterIsInstance<BattleEvent.VolatileStatusCleared>().size)
	}

	private fun scenario(
		name: String,
		inputSummary: String,
		expectedSummary: String,
	): PublicBattleRuleScenario =
		publicBattleRuleScenario(
			name = name,
			inputSummary = inputSummary,
			expectedSummary = expectedSummary,
		)

	private fun statOperationSkill(
		skillId: Long,
		operation: BattleStatStageOperation,
	): BattleSkillSlot =
		damagingSkill(
			skillId = skillId,
			name = "能力阶级操作测试",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			affectedByProtect = false,
			statStageOperations = listOf(operation),
		)
}
