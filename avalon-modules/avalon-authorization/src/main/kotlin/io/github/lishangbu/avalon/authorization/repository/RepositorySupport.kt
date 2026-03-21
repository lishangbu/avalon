package io.github.lishangbu.avalon.authorization.repository

internal fun String?.takeFilter(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

internal inline fun <T, R> T?.readOrNull(block: T.() -> R): R? = this?.let { runCatching { it.block() }.getOrNull() }
