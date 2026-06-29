package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.random.BattleRandom

/**
 * 对同一排序键下的成员使用确定性随机数打破平手。
 *
 * 该函数只服务“排序键完全相同”的稳定洗牌，不负责计算优先度、速度或其它排序键。调用方传入每个元素消费随机数
 * 时写入 replay 的原因文本，确保同一初始状态、行动序列和随机序列可以复现完全一致的行动顺序。
 */
internal fun <T> List<T>.sortedByRandomTieBreak(random: BattleRandom, reason: (T) -> String): List<T> =
	map { item -> item to random.nextInt(1_000_000, reason(item)) }
		.sortedBy { it.second }
		.map { it.first }
