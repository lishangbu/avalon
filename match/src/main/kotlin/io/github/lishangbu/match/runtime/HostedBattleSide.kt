package io.github.lishangbu.match.runtime

/** 一方固定顺序的成员以及零基 Lead 索引。 */
data class HostedBattleSide(
	val activeParticipantIndexes: List<Int>,
	val participants: List<HostedBattleParticipant>,
)
