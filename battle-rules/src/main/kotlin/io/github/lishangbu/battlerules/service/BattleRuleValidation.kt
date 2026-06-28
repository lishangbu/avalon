package io.github.lishangbu.battlerules.service

import io.github.lishangbu.common.web.invalidValue
import io.github.lishangbu.common.web.invalidReference
import io.github.lishangbu.common.web.requiredText
import org.springframework.jdbc.core.JdbcTemplate

/**
 * 战斗规则管理服务共用的输入清洗函数。
 *
 * 这些函数只处理基础字段约束，不封装任何表级 CRUD 流程；每张规则表仍由自己的 Service
 * 决定唯一性、外键引用和响应转换，避免回到通用表服务模式。
 */
internal fun optionalText(value: String?, fieldName: String, maxLength: Int): String? {
	val text = value.orEmpty().trim()
	if (text.isBlank()) {
		return null
	}
	if (text.length > maxLength) {
		invalidValue(fieldName, "$fieldName 长度不能超过 $maxLength")
	}
	return text
}

internal fun requiredUpperText(value: String, fieldName: String, maxLength: Int): String =
	value.requiredText(fieldName, maxLength).uppercase()

internal fun requiredEnumText(value: String, fieldName: String, allowedValues: Set<String>): String {
	val text = requiredUpperText(value, fieldName, maxLength = 40)
	if (text !in allowedValues) {
		invalidValue(fieldName, "$fieldName 只能是 ${allowedValues.joinToString()}")
	}
	return text
}

internal fun requiredPolicyCode(value: String, fieldName: String): String {
	val text = value.requiredText(fieldName, maxLength = 80)
	if (!POLICY_CODE_PATTERN.matches(text)) {
		invalidValue(fieldName, "$fieldName 只能包含小写字母、数字和连字符")
	}
	return text
}

internal fun requiredPositiveId(value: Long, fieldName: String): Long {
	if (value <= 0) {
		invalidValue(fieldName, "$fieldName 必须大于 0")
	}
	return value
}

internal fun requiredIntRange(value: Int, fieldName: String, min: Int, max: Int): Int {
	if (value !in min..max) {
		invalidValue(fieldName, "$fieldName 必须在 $min 到 $max 之间")
	}
	return value
}

internal fun optionalIntRange(value: Int?, fieldName: String, min: Int, max: Int): Int? {
	if (value == null) {
		return null
	}
	return requiredIntRange(value, fieldName, min, max)
}

internal fun requireExistingGameDataReference(
	jdbcTemplate: JdbcTemplate,
	tableName: String,
	id: Long,
	fieldName: String,
	displayName: String,
) {
	val exists = jdbcTemplate.queryForObject(
		"select exists(select 1 from $tableName where id = ?)",
		Boolean::class.java,
		id,
	) == true
	if (!exists) {
		invalidReference(fieldName, "$displayName 不存在: $id")
	}
}

private val POLICY_CODE_PATTERN = Regex("^[a-z][a-z0-9-]{1,79}$")
