package io.github.lishangbu.match.trainer

/** 已从启用规则记录投影出的名称审核规则。 */
data class SensitiveNameRule(val normalizedTerm: String, val matchType: SensitiveNameMatchType)
