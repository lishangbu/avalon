package io.github.lishangbu.gamedata.support

import io.github.lishangbu.common.web.filterValue
import io.github.lishangbu.common.web.invalidValue

/**
 * 规范化必填资料文本。
 */
internal fun gameDataRequiredText(value: String?, fieldName: String, maxLength: Int?): String {
	val normalized = value.orEmpty().trim()
	if (normalized.isEmpty()) {
		invalidValue(fieldName, "$fieldName 不能为空")
	}
	if (maxLength != null && normalized.length > maxLength) {
		invalidValue(fieldName, "$fieldName 长度不能超过 $maxLength")
	}
	return normalized
}

/**
 * 规范化可空资料文本。
 */
internal fun gameDataOptionalText(value: String?, fieldName: String, maxLength: Int?): String? {
	val normalized = value.orEmpty().trim()
	if (normalized.isEmpty()) {
		return null
	}
	if (maxLength != null && normalized.length > maxLength) {
		invalidValue(fieldName, "$fieldName 长度不能超过 $maxLength")
	}
	return normalized
}

internal fun gameDataStringFilterValue(fieldName: String, value: String): String? =
	filterValue(fieldName, value)

internal fun gameDataLongFilterValue(fieldName: String, value: String): Long? =
	filterValue(fieldName, value)?.let { normalized ->
		normalized.toLongOrNull() ?: invalidValue(fieldName, "$fieldName 必须是整数")
	}

internal fun gameDataIntFilterValue(fieldName: String, value: String): Int? =
	filterValue(fieldName, value)?.let { normalized ->
		normalized.toIntOrNull() ?: invalidValue(fieldName, "$fieldName 必须是整数")
	}

internal fun gameDataBooleanFilterValue(fieldName: String, value: String): Boolean? =
	filterValue(fieldName, value)?.let { normalized ->
		when (normalized.lowercase()) {
			"true" -> true
			"false" -> false
			else -> invalidValue(fieldName, "$fieldName 必须是布尔值")
		}
	}
