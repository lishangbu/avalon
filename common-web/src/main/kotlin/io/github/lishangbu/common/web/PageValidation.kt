package io.github.lishangbu.common.web

import org.babyfish.jimmer.Page
import org.springframework.http.HttpStatus

private const val DEFAULT_PAGE_SIZE = 50
private const val MAX_PAGE_SIZE = 100
private const val MAX_QUERY_LENGTH = 80

/**
 * 校验后台 API 分页参数。
 */
fun validatePage(page: Int, size: Int = DEFAULT_PAGE_SIZE) {
	if (page < 0) {
		invalidPageField("page", "page 不能小于 0")
	}
	if (size !in 1..MAX_PAGE_SIZE) {
		invalidPageField("size", "size 必须在 1 到 $MAX_PAGE_SIZE 之间")
	}
}

/**
 * 校验并规范化后台列表搜索词。
 */
fun searchFilter(query: String?): SearchFilter {
	val value = filterValue("q", query)
	if (value == null) {
		return SearchFilter(pattern = null)
	}
	return SearchFilter(pattern = "%${value.lowercase()}%")
}

/**
 * 规范化后台列表的可选筛选值。
 */
fun filterValue(fieldName: String, value: String?): String? {
	val text = value.orEmpty().trim()
	if (text.isBlank()) {
		return null
	}
	if (text.length > MAX_QUERY_LENGTH) {
		invalidPageField(fieldName, "$fieldName 长度不能超过 $MAX_QUERY_LENGTH")
	}
	return text
}

/**
 * 转换 Jimmer 分页中的行数据，同时保留 Jimmer 原生分页元数据。
 */
fun <T, R> Page<T>.mapRows(transform: (T) -> R): Page<R> =
	Page(rows.map(transform), totalRowCount, totalPageCount)

private fun invalidPageField(fieldName: String, message: String): Nothing =
	throw ApiException(
		status = HttpStatus.BAD_REQUEST,
		code = ApiErrorCode.VALIDATION_INVALID,
		message = message,
		field = fieldName,
	)
