package io.github.lishangbu.avalon.identity.access.domain.authentication

import io.github.lishangbu.avalon.identity.access.domain.iam.IdentityAccessBadRequest

/**
 * 归一化用户名登录标识。
 *
 * @param value 原始用户名输入。
 * @return 去空格并转为小写后的用户名。
 */
fun normalizeUsername(value: String): String = value.trim().lowercase()

/**
 * 归一化邮箱登录标识。
 *
 * @param value 原始邮箱输入。
 * @return 去空格、空串归零并转为小写后的邮箱；如果输入为空则返回 `null`。
 */
fun normalizeEmail(value: String?): String? = value?.trim()?.takeIf { it.isNotEmpty() }?.lowercase()

/**
 * 归一化手机号登录标识。
 *
 * @param value 原始手机号输入。
 * @return 仅保留数字，若原值显式带 `+` 则保留国际区号前缀；无法提取有效数字时返回 `null`。
 */
fun normalizePhone(value: String?): String? {
    val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val digits = trimmed.filter { it.isDigit() }
    return when {
        trimmed.startsWith("+") && digits.isNotEmpty() -> "+$digits"
        digits.isNotEmpty() -> digits
        else -> null
    }
}

/**
 * 按身份类型归一化登录 principal。
 *
 * 设计上归一化发生在认证前，保证仓储查询和审计日志使用统一格式，
 * 避免同一用户因为大小写、空格或手机号格式差异而出现多种匹配结果。
 *
 * @param identityType 登录标识类型。
 * @param principal 原始登录 principal。
 * @return 归一化后的 principal。
 * @throws IdentityAccessBadRequest 当 principal 为空或与身份类型不匹配时抛出。
 */
fun normalizePrincipal(
    identityType: IdentityType,
    principal: String,
): String {
    val trimmed = principal.trim()
    if (trimmed.isEmpty()) {
        throw IdentityAccessBadRequest("principal must not be blank")
    }
    return when (identityType) {
        IdentityType.USERNAME -> normalizeUsername(trimmed)
        IdentityType.EMAIL -> normalizeEmail(trimmed) ?: badRequest("email principal must not be blank")
        IdentityType.PHONE -> normalizePhone(trimmed) ?: badRequest("phone principal must contain digits.")
    }
}

private fun badRequest(message: String): Nothing = throw IdentityAccessBadRequest(message)