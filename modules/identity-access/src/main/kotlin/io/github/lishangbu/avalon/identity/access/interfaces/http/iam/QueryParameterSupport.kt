package io.github.lishangbu.avalon.identity.access.interfaces.http.iam

internal fun String?.normalizeQueryText(): String? = this?.trim()?.takeIf { it.isNotEmpty() }