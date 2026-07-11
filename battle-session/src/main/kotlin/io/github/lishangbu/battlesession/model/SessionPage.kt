package io.github.lishangbu.battlesession.model

/** 返回 Runtime 内活跃与 Recent Session 的稳定分页切片。 */
data class SessionPage(
	val items: List<BattleSessionSummary>,
	val totalElements: Long,
	val page: Int,
	val size: Int,
) {
	val totalPages: Int =
		if (totalElements == 0L) 0 else ((totalElements - 1) / size + 1).toInt()
}
