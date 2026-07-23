package io.github.lishangbu.match.trainer

import io.github.lishangbu.battleengine.model.BattleGender

/** Team 完整替换请求中的单个成员。Identifier 在 JSON 中使用十进制字符串。 */
data class SaveTrainerTeamMemberRequest(
	var creatureId: String = "",
	var gender: BattleGender? = null,
	var skinId: String = "",
	var skillIds: List<String> = emptyList(),
	var abilityId: String = "",
	var itemId: String = "",
	var natureId: String? = null,
	var teraElementId: String = "",
	var individualValues: Map<String, Int> = emptyMap(),
	var effortValues: Map<String, Int> = emptyMap(),
)
