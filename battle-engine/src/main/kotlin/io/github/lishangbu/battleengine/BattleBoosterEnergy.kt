package io.github.lishangbu.battleengine

import io.github.lishangbu.battleengine.model.BattleParticipant
import io.github.lishangbu.battleengine.model.BattleStat

/** 返回驱劲能量运行时标记对指定能力的倍率。 */
internal fun BattleParticipant.boosterEnergyMultiplier(stat: BattleStat): Double =
	if (boosterEnergyStat != stat) 1.0 else if (stat == BattleStat.SPEED) 1.5 else 1.3
