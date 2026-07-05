package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleAction
import io.github.lishangbu.battleengine.model.BattleDamageClass
import io.github.lishangbu.battleengine.model.BattleEffectTarget
import io.github.lishangbu.battleengine.model.BattleEvent
import io.github.lishangbu.battleengine.model.BattleMajorStatus
import io.github.lishangbu.battleengine.model.BattleSideConditionTarget
import io.github.lishangbu.battleengine.model.BattleSideProtection
import io.github.lishangbu.battleengine.model.BattleSideProtectionApplication
import io.github.lishangbu.battleengine.model.BattleSideProtectionKind
import io.github.lishangbu.battleengine.model.BattleSkillTargetScope
import io.github.lishangbu.battleengine.model.BattleStat
import io.github.lishangbu.battleengine.model.BattleStatStageEffect
import io.github.lishangbu.battleengine.model.BattleStatusApplication
import io.github.lishangbu.battleengine.model.BattleStatusBlockReason
import io.github.lishangbu.battleengine.model.BattleVolatileStatus
import io.github.lishangbu.battleengine.model.BattleVolatileStatusApplication
import io.github.lishangbu.battleengine.random.ScriptedBattleRandom
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 验证聚气、白雾和神秘守护这一组在场/一侧防护规则。
 *
 * 场景类型：命中后状态建立与后续阻止条件 场景。
 * 参考来源类型：公开成熟模拟器实现和公开规则说明。现代规则中，聚气提高使用者在场期间的要害等级；白雾防止
 * 己方被其它成员降低能力阶级；神秘守护防止己方被其它成员附加主要异常状态或混乱，但不阻止使用者自我状态。
 * 验证重点：这些效果必须写入强类型运行态，并在后续普通结算入口被读取，而不是只把技能标记为可选。
 */
class BattleFocusEnergyAndSideProtectionTests {
	private val engine = BattleEngine()

	@Test
	fun `focus energy bonus combines with high critical hit skill`() {
		val scenario = publicBattleRuleScenario(
			name = "focus-energy-bonus-combines-with-high-critical-hit-skill",
			inputSummary = "使用者先成功使用聚气，再使用基础要害等级 +1 的伤害技能。",
			expectedSummary = "成员获得 +2 要害等级加成，后续技能总等级达到 +3 后必定击中要害，且不消费要害随机数。",
		)
		val focusEnergy = focusEnergySkill()
		val highCriticalHitSkill = damagingSkill(skillId = 2, name = "高要害测试", criticalHitStage = 1)
		val user = participant("focus-user", speed = 100, skill = focusEnergy)
			.copy(skillSlots = listOf(focusEnergy, highCriticalHitSkill))
		val started = engine.start(
			initialState(
				first = user,
				second = participant("target", speed = 50),
			),
		)

		val afterFocus = engine.resolveTurn(
			started,
			listOf(BattleAction.UseSkill("focus-user", skillId = 116, targetActorId = "focus-user")),
			ScriptedBattleRandom(emptyList()),
		)
		val random = ScriptedBattleRandom(listOf(15))
		val resolved = engine.resolveTurn(
			afterFocus,
			listOf(BattleAction.UseSkill("focus-user", skillId = 2, targetActorId = "target")),
			random,
		)

		scenario.assertNamed("focus-energy-bonus-combines-with-high-critical-hit-skill")
		val boost = afterFocus.events.filterIsInstance<BattleEvent.CriticalHitStageBoostStarted>().single()
		assertEquals("focus-user", boost.actorId)
		assertEquals(2, boost.stageBonus)
		assertEquals(2, resolved.participant("focus-user")?.criticalHitStageBonus)
		val damage = resolved.events.filterIsInstance<BattleEvent.DamageApplied>().single { it.skillId == 2L }
		assertTrue(damage.criticalHit)
		assertEquals(listOf("damage random for 2"), random.consumedReasons())
	}

	@Test
	fun `mist side protection blocks stat stage drops from another participant`() {
		val scenario = publicBattleRuleScenario(
			name = "mist-side-protection-blocks-stat-stage-drops-from-another-participant",
			inputSummary = "使用者先建立白雾，下一回合对手使用降低攻击阶级的变化技能。",
			expectedSummary = "使用者所在侧拥有能力下降防护，对手的降攻击效果被记录为一侧防护阻止，目标能力阶级不变。",
		)
		val mist = sideProtectionSkill(
			skillId = 54,
			name = "白雾",
			kind = BattleSideProtectionKind.STAT_STAGE_REDUCTION,
		)
		val growl = damagingSkill(
			skillId = 45,
			name = "叫声",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			statStageEffects = listOf(
				BattleStatStageEffect(
					stat = BattleStat.ATTACK,
					target = BattleEffectTarget.TARGET,
					stageDelta = -1,
					chancePercent = 100,
				),
			),
		)
		val afterMist = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("mist-user", speed = 100, skill = mist),
					second = participant("stat-dropper", speed = 50, skill = growl),
				),
			),
			listOf(BattleAction.UseSkill("mist-user", skillId = 54, targetActorId = "mist-user")),
			ScriptedBattleRandom(emptyList()),
		)
		val resolved = engine.resolveTurn(
			afterMist,
			listOf(BattleAction.UseSkill("stat-dropper", skillId = 45, targetActorId = "mist-user")),
			ScriptedBattleRandom(emptyList()),
		)

		scenario.assertNamed("mist-side-protection-blocks-stat-stage-drops-from-another-participant")
		val protection = afterMist.events.filterIsInstance<BattleEvent.SideProtectionStarted>().single()
		assertEquals(BattleSideProtectionKind.STAT_STAGE_REDUCTION, protection.kind)
		assertEquals(5, protection.turnsRemaining)
		assertEquals(0, resolved.participant("mist-user")?.statStage(BattleStat.ATTACK))
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatStageChanged>())
		val blocked = resolved.events.filterIsInstance<BattleEvent.StatStageChangeBlocked>().single()
		assertEquals(BattleStat.ATTACK, blocked.stat)
		assertEquals(-1, blocked.attemptedDelta)
		assertEquals(BattleStatusBlockReason.SIDE_PROTECTION, blocked.reason)
	}

	@Test
	fun `safeguard blocks major status and confusion from another participant`() {
		val scenario = publicBattleRuleScenario(
			name = "safeguard-blocks-major-status-and-confusion-from-another-participant",
			inputSummary = "目标所在侧已有神秘守护，对手分别尝试附加灼伤和混乱。",
			expectedSummary = "主要异常状态和混乱都被一侧防护阻止，状态私有随机数不会被消费。",
		)
		val burnSkill = statusSkill(BattleMajorStatus.BURN)
		val confuseSkill = damagingSkill(
			skillId = 48,
			name = "超音波",
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
		val targetSideProtection = listOf(
			BattleSideProtection(BattleSideProtectionKind.STATUS_CONDITION, turnsRemaining = 5),
		)

		val burnResolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("status-user", speed = 100, skill = burnSkill),
					second = participant("guarded-target", speed = 50),
					secondSideProtections = targetSideProtection,
				),
			),
			listOf(BattleAction.UseSkill("status-user", skillId = 1, targetActorId = "guarded-target")),
			ScriptedBattleRandom(emptyList()),
		)
		val random = ScriptedBattleRandom(emptyList())
		val confusionResolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("confuse-user", speed = 100, skill = confuseSkill),
					second = participant("guarded-target", speed = 50),
					secondSideProtections = targetSideProtection,
				),
			),
			listOf(BattleAction.UseSkill("confuse-user", skillId = 48, targetActorId = "guarded-target")),
			random,
		)

		scenario.assertNamed("safeguard-blocks-major-status-and-confusion-from-another-participant")
		assertEquals(null, burnResolved.participant("guarded-target")?.majorStatus)
		val burnBlocked = burnResolved.events.filterIsInstance<BattleEvent.StatusApplicationBlocked>().single()
		assertEquals(BattleStatusBlockReason.SIDE_PROTECTION, burnBlocked.reason)
		assertEquals(0, confusionResolved.participant("guarded-target")?.confusionTurnsRemaining)
		val confusionBlocked = confusionResolved.events.filterIsInstance<BattleEvent.VolatileStatusApplicationBlocked>().single()
		assertEquals(BattleStatusBlockReason.SIDE_PROTECTION, confusionBlocked.reason)
		assertEquals(emptyList(), random.consumedReasons())
	}

	@Test
	fun `safeguard does not block self applied major status`() {
		val scenario = publicBattleRuleScenario(
			name = "safeguard-does-not-block-self-applied-major-status",
			inputSummary = "神秘守护存在时，使用者用自我状态技能让自己睡眠。",
			expectedSummary = "一侧防护不阻止成员自身给自己附加主要异常状态，并正常消费睡眠持续随机数。",
		)
		val selfSleep = damagingSkill(
			skillId = 156,
			name = "睡觉",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			targetScope = BattleSkillTargetScope.SELF,
			statusApplications = listOf(
				BattleStatusApplication(
					status = BattleMajorStatus.SLEEP,
					target = BattleEffectTarget.USER,
					chancePercent = 100,
				),
			),
		)
		val random = ScriptedBattleRandom(listOf(1))
		val resolved = engine.resolveTurn(
			engine.start(
				initialState(
					first = participant("self-status-user", speed = 100, skill = selfSleep),
					second = participant("observer", speed = 50),
					firstSideProtections = listOf(
						BattleSideProtection(BattleSideProtectionKind.STATUS_CONDITION, turnsRemaining = 5),
					),
				),
			),
			listOf(BattleAction.UseSkill("self-status-user", skillId = 156, targetActorId = "self-status-user")),
			random,
		)

		scenario.assertNamed("safeguard-does-not-block-self-applied-major-status")
		assertEquals(BattleMajorStatus.SLEEP, resolved.participant("self-status-user")?.majorStatus)
		assertEquals(listOf("sleep duration for 156"), random.consumedReasons())
		assertEquals(emptyList(), resolved.events.filterIsInstance<BattleEvent.StatusApplicationBlocked>())
	}

	private fun focusEnergySkill() =
		damagingSkill(
			skillId = 116,
			name = "聚气",
			damageClass = BattleDamageClass.STATUS,
			power = null,
			targetScope = BattleSkillTargetScope.SELF,
			affectedByProtect = false,
			criticalHitStageBoost = 2,
		)

	private fun statusSkill(status: BattleMajorStatus) =
		damagingSkill(
			damageClass = BattleDamageClass.STATUS,
			power = null,
			statusApplications = listOf(
				BattleStatusApplication(
					status = status,
					target = BattleEffectTarget.TARGET,
					chancePercent = 100,
				),
			),
		)

	private fun sideProtectionSkill(
		skillId: Long,
		name: String,
		kind: BattleSideProtectionKind,
	) =
		damagingSkill(
			skillId = skillId,
			name = name,
			damageClass = BattleDamageClass.STATUS,
			power = null,
			targetScope = BattleSkillTargetScope.SELF,
			affectedByProtect = false,
			sideProtectionApplications = listOf(
				BattleSideProtectionApplication(
					targetSide = BattleSideConditionTarget.USER_SIDE,
					protection = BattleSideProtection(kind = kind, turnsRemaining = 5),
				),
			),
		)
}
