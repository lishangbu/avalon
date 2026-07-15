package io.github.lishangbu.common.web

import org.springframework.http.HttpStatus

private val slugCodePattern = Regex("^[a-z][a-z0-9-]{2,63}$")
private val usernamePattern = Regex("^[a-z][a-z0-9._-]{2,63}$")
private val accessNodeCodePattern = Regex("^[a-z][a-z0-9-]*(?:[.:][a-z][a-z0-9-]*)+$")

/**
 * 读取必填文本字段，并限制最大长度。
 */
fun String.requiredText(fieldName: String, maxLength: Int, minLength: Int = 1): String {
	val value = trim()
	if (value.isBlank()) {
		throw requiredField(fieldName)
	}
	if (value.length < minLength) {
		throw invalidField(fieldName, "$fieldName 长度不能少于 $minLength")
	}
	if (value.length > maxLength) {
		throw invalidField(fieldName, "$fieldName 长度不能超过 $maxLength")
	}
	return value
}

/**
 * 读取稳定 slug/code 字段。
 */
fun String.requiredSlugCode(fieldName: String): String {
	val value = requiredText(fieldName, maxLength = 64)
	if (!slugCodePattern.matches(value)) {
		throw invalidField(fieldName, "$fieldName 必须以小写字母开头，只能包含小写字母、数字和连字符，长度为 3 到 64")
	}
	return value
}

/**
 * 读取用户名字段。
 */
fun String.requiredUsername(fieldName: String): String {
	val value = requiredText(fieldName, maxLength = 64)
	if (!usernamePattern.matches(value)) {
		throw invalidField(fieldName, "$fieldName 必须以小写字母开头，只能包含小写字母、数字、点、下划线和连字符，长度为 3 到 64")
	}
	return value
}

/**
 * 读取密码字段，并限制长度。
 */
fun String.requiredPassword(fieldName: String): String {
	if (isBlank()) {
		throw requiredField(fieldName)
	}
	if (length < 8) {
		throw invalidField(fieldName, "$fieldName 长度不能少于 8")
	}
	if (length > 128) {
		throw invalidField(fieldName, "$fieldName 长度不能超过 128")
	}
	return this
}

/**
 * 规范化访问节点 code 列表。
 */
fun List<String>.normalizedAccessNodeCodes(fieldName: String): List<String> {
	val values = normalizedList(fieldName)
	val invalidValue = values.firstOrNull { !accessNodeCodePattern.matches(it) }
	if (invalidValue != null) {
		throw invalidField(fieldName, "$fieldName 包含非法访问节点 code: $invalidValue")
	}
	return values
}

/**
 * 规范化 slug/code 列表。
 */
fun List<String>.normalizedSlugCodes(fieldName: String): List<String> {
	val values = normalizedList(fieldName)
	val invalidValue = values.firstOrNull { !slugCodePattern.matches(it) }
	if (invalidValue != null) {
		throw invalidField(fieldName, "$fieldName 包含非法 code: $invalidValue")
	}
	return values
}

/**
 * 确保字段值属于允许集合。
 */
fun String.requiredSupportedValue(fieldName: String, supportedValues: Set<String>): String {
	val value = requiredText(fieldName, maxLength = 64)
	if (value !in supportedValues) {
		throw unsupportedField(fieldName, "$fieldName 不支持: $value")
	}
	return value
}

/**
 * 确保列表全部属于允许集合。
 */
fun List<String>.requireSupportedValues(fieldName: String, supportedValues: Set<String>) {
	val unsupportedValues = filterNot(supportedValues::contains)
	if (unsupportedValues.isNotEmpty()) {
		throw unsupportedField(fieldName, "$fieldName 不支持: ${unsupportedValues.joinToString()}")
	}
}

/**
 * 校验数值字段处于闭区间范围内。
 */
fun Long.requiredRange(fieldName: String, min: Long, max: Long): Long {
	if (this !in min..max) {
		throw invalidField(fieldName, "$fieldName 必须在 $min 到 $max 之间")
	}
	return this
}

/**
 * 抛出字段非法错误。
 */
fun invalidValue(fieldName: String, message: String): Nothing =
	throw invalidField(fieldName, message)

/**
 * 抛出资源冲突错误。
 */
fun conflict(fieldName: String, message: String): Nothing =
	throw ApiException(
		status = HttpStatus.CONFLICT,
		code = ApiErrorCode.RESOURCE_CONFLICT,
		message = message,
		field = fieldName,
	)

/**
 * 抛出关联资源不存在或不可用错误。
 */
fun invalidReference(fieldName: String, message: String): Nothing =
	throw invalidField(fieldName, message)

/**
 * 抛出资源不存在错误。
 */
fun notFound(fieldName: String, message: String): Nothing =
	throw ApiException(
		status = HttpStatus.NOT_FOUND,
		code = ApiErrorCode.RESOURCE_NOT_FOUND,
		message = message,
		field = fieldName,
	)

private fun List<String>.normalizedList(fieldName: String): List<String> {
	val values = map(String::trim)
	if (values.any(String::isBlank)) {
		throw invalidField(fieldName, "$fieldName 不能包含空值")
	}
	val seenValues = mutableSetOf<String>()
	val duplicatedValue = values.firstOrNull { !seenValues.add(it) }
	if (duplicatedValue != null) {
		throw invalidField(fieldName, "$fieldName 不能包含重复值: $duplicatedValue")
	}
	return values.sorted()
}

private fun requiredField(fieldName: String): ApiException =
	ApiException(
		status = HttpStatus.BAD_REQUEST,
		code = ApiErrorCode.VALIDATION_REQUIRED,
		message = "$fieldName 不能为空",
		field = fieldName,
	)

private fun invalidField(fieldName: String, message: String): ApiException =
	ApiException(
		status = HttpStatus.BAD_REQUEST,
		code = ApiErrorCode.VALIDATION_INVALID,
		message = message,
		field = fieldName,
	)

private fun unsupportedField(fieldName: String, message: String): ApiException =
	ApiException(
		status = HttpStatus.BAD_REQUEST,
		code = ApiErrorCode.VALIDATION_UNSUPPORTED,
		message = message,
		field = fieldName,
	)
