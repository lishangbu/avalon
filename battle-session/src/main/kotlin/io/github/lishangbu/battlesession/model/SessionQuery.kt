package io.github.lishangbu.battlesession.model

/** 描述 Runtime 会话列表的筛选与分页条件。 */
data class SessionQuery(
	val status: BattleSessionStatus? = null,
	val formatCode: String? = null,
	val page: Int = 0,
	val size: Int = 20,
) {
	init {
		require(formatCode == null || formatCode.isNotBlank()) { "formatCode must not be blank when present" }
		require(page >= 0) { "page must not be negative" }
		require(size in 1..200) { "size must be between 1 and 200" }
	}
}
