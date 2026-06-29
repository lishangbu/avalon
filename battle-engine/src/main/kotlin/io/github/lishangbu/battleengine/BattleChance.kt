package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleSkillSlot
import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 结算百分比概率。
 *
 * 100% 不消费随机数，0% 永远失败；中间概率消费 1..100 掷点。调用方负责提供 replay 可读的随机原因文本，让公开
 * fixture 能验证随机消费顺序。
 */
internal fun chanceSucceeds(chancePercent: Int, random: BattleRandom, reason: String): Boolean =
	when (chancePercent) {
		100 -> true
		0 -> false
		else -> random.nextInt(100, reason) + 1 <= chancePercent
	}

/**
 * 决定本次技能使用的实际命中段数。
 *
 * 单段技能不消费随机数。现代 2..5 段技能使用公开规则中的非均匀分布：2 段和 3 段各 35%，4 段和 5 段各 15%。
 * 其它自定义范围暂按均匀分布处理，便于未来接入固定 2 段或资料驱动特殊段数时仍有确定行为。
 */
internal fun determineHitCount(skill: BattleSkillSlot, random: BattleRandom): Int {
	if (skill.minHits == skill.maxHits) {
		return skill.minHits
	}
	if (skill.minHits == 2 && skill.maxHits == 5) {
		val roll = random.nextInt(100, "multi-hit count for ${skill.skillId}")
		return when {
			roll < 35 -> 2
			roll < 70 -> 3
			roll < 85 -> 4
			else -> 5
		}
	}
	return skill.minHits + random.nextInt(skill.maxHits - skill.minHits + 1, "multi-hit count for ${skill.skillId}")
}

/**
 * 结算击中要害概率。
 *
 * 现代规则下，普通等级概率为 1/24，+1 为 1/8，+2 为 1/2，+3 及以上视为必定击中要害。必定要害不消费随机数；
 * 其它等级消费 `[0, denominator)`，掷到 0 表示成功。
 */
internal fun criticalHitCheck(skill: BattleSkillSlot, random: BattleRandom): CriticalHitCheck {
	val denominator = when (skill.criticalHitStage.coerceAtMost(3)) {
		0 -> 24
		1 -> 8
		2 -> 2
		else -> 1
	}
	if (denominator == 1) {
		return CriticalHitCheck(hit = true, roll = null)
	}
	val roll = random.nextInt(denominator, "critical hit for ${skill.skillId}")
	return CriticalHitCheck(hit = roll == 0, roll = roll)
}

/**
 * 结算保护类行动的连续使用成功率。
 *
 * 第一次保护必定成功；如果上一回合已经成功保护过，则下一次按 `1 / 3^chain` 掷点，例如第二次 1/3、第三次
 * 1/9。该函数只负责概率，不消耗 PP，也不修改战斗状态。
 */
internal fun protectionSucceeds(actor: BattleParticipant, skill: BattleSkillSlot, random: BattleRandom): Boolean {
	val denominator = protectionChanceDenominator(actor.protectionChain)
	if (denominator == 1) {
		return true
	}
	return random.nextInt(denominator, "protection chance for ${skill.skillId}") == 0
}

/**
 * 根据已经连续成功保护的次数计算下一次保护成功率分母。
 *
 * 分母按 3 的幂增长，并夹在 Int 范围内，避免极端测试 fixture 构造出不可表示的概率。
 */
private fun protectionChanceDenominator(chain: Int): Int {
	var denominator = 1
	repeat(chain) {
		denominator = (denominator * 3L).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
	}
	return denominator
}

internal data class CriticalHitCheck(
	val hit: Boolean,
	val roll: Int?,
)
