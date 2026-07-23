package io.github.lishangbu.match.challenge

import io.github.lishangbu.battleengine.model.BattleGender

/** 不可变队伍成员；仅服务端 Match Runtime 使用，不进入 Challenge 对手视图。 */
data class TrainerTeamSnapshotMember(
	var creatureId: Long = 0,
	var gender: BattleGender = BattleGender.GENDERLESS,
	var skinId: Long = 0,
	var skillIds: List<Long> = emptyList(),
	var abilityId: Long = 0,
	var itemId: Long = 0,
	var natureId: Long = 0,
	var teraElementId: Long = 0,
	var level: Int = 50,
	var individualValues: Map<String, Int> = emptyMap(),
	var effortValues: Map<String, Int> = emptyMap(),
)
