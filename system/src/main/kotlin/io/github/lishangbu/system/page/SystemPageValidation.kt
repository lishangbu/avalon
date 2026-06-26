package io.github.lishangbu.system.page

import io.github.lishangbu.system.error.SystemApiErrorCode
import io.github.lishangbu.system.error.SystemApiException
import org.babyfish.jimmer.Page
import org.springframework.http.HttpStatus

private const val DEFAULT_PAGE_SIZE = 50
private const val MAX_PAGE_SIZE = 100
private const val MAX_QUERY_LENGTH = 80

/**
 * 校验系统管理分页参数。
 */
internal fun validateSystemPage(page: Int, size: Int = DEFAULT_PAGE_SIZE) {
	if (page < 0) {
		invalidPageField("page", "page 不能小于 0")
	}
	if (size !in 1..MAX_PAGE_SIZE) {
		invalidPageField("size", "size 必须在 1 到 $MAX_PAGE_SIZE 之间")
	}
}

/**
 * 校验并规范化系统管理列表搜索词。
 */
internal fun systemSearchFilter(query: String?): SystemSearchFilter {
	val value = systemFilterValue("q", query)
	if (value == null) {
		return SystemSearchFilter(pattern = null)
	}
	return SystemSearchFilter(pattern = "%${value.lowercase()}%")
}

/**
 * 规范化系统管理列表的可选筛选值。
 */
internal fun systemFilterValue(fieldName: String, value: String?): String? {
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
internal fun <T, R> Page<T>.mapRows(transform: (T) -> R): Page<R> =
	Page(rows.map(transform), totalRowCount, totalPageCount)

private fun invalidPageField(fieldName: String, message: String): Nothing =
	throw SystemApiException(
		status = HttpStatus.BAD_REQUEST,
		code = SystemApiErrorCode.VALIDATION_INVALID,
		message = message,
		field = fieldName,
	)
