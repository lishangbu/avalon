package io.github.lishangbu.common.web

/** Parses an externally stringified database identifier used in a path. */
fun String.pathIdentifier(fieldName: String): Long =
	toLongOrNull()?.takeIf { it > 0 }
		?: notFound(fieldName, "$fieldName resource does not exist")

/** Parses an optional descending-pagination cursor; absence means the beginning of the feed. */
fun String?.queryCursorIdentifier(fieldName: String): Long =
	this?.toLongOrNull()?.takeIf { it > 0 }
		?: if (this == null) Long.MAX_VALUE else invalidValue(fieldName, "$fieldName must be a positive integer")
