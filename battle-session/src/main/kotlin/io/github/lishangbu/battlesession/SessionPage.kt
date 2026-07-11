package io.github.lishangbu.battlesession

data class SessionPage(
	val items: List<BattleSessionSummary>,
	val totalElements: Long,
	val page: Int,
	val size: Int,
) {
	val totalPages: Int =
		if (totalElements == 0L) 0 else ((totalElements - 1) / size + 1).toInt()
}
