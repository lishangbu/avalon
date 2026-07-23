package io.github.lishangbu.battleengine.model

/** 一项条件形态规则中的基础形态与替代形态资料 code。 */
data class BattleFormPair(
	val baseFormCode: String,
	val alternateFormCode: String,
) {
	init {
		require(baseFormCode.isNotBlank()) { "baseFormCode must not be blank" }
		require(alternateFormCode.isNotBlank()) { "alternateFormCode must not be blank" }
		require(baseFormCode != alternateFormCode) { "form codes must differ" }
	}
}
