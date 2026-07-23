package io.github.lishangbu.match.runtime

import io.github.lishangbu.battleengine.model.BattleGender

/** Battle Session 启动所需成员资料；性格已解析为能力 code，Runtime 无需读取 Match 持久层。 */
data class HostedBattleParticipant(
	val creatureId: Long,
	val gender: BattleGender,
	val level: Int,
	val skillIds: List<Long>,
	val abilityId: Long,
	val itemId: Long,
	val individualValues: Map<String, Int>,
	val effortValues: Map<String, Int>,
	val natureIncreasedStat: String?,
	val natureDecreasedStat: String?,
	val teraElementId: Long? = null,
)
