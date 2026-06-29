package io.github.lishangbu.battleengine

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
