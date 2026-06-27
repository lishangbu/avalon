package io.github.lishangbu.gamedata.support

import jakarta.servlet.http.HttpServletRequest

private val gameDataListReservedParameters = setOf("page", "size", "q")

/**
 * 提取游戏资料列表的字段筛选参数。
 *
 * page、size 和 q 是通用分页搜索参数，其余查询参数交给对应资源的持久层白名单校验。
 */
internal fun HttpServletRequest.toGameDataFilters(): Map<String, String> =
	parameterMap
		.filterKeys { parameter -> parameter !in gameDataListReservedParameters }
		.mapValues { (_, values) -> values.firstOrNull().orEmpty() }
