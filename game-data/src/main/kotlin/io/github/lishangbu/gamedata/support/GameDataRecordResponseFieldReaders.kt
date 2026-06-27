package io.github.lishangbu.gamedata.support

import io.github.lishangbu.gamedata.model.GameDataRecordResponse

internal fun GameDataRecordResponse.stringField(name: String): String? =
	fields[name]?.toString()

internal fun GameDataRecordResponse.longField(name: String): Long? =
	when (val value = fields[name]) {
		is Number -> value.toLong()
		is String -> value.toLongOrNull()
		else -> null
	}

internal fun GameDataRecordResponse.intField(name: String): Int? =
	when (val value = fields[name]) {
		is Number -> value.toInt()
		is String -> value.toIntOrNull()
		else -> null
	}

internal fun GameDataRecordResponse.booleanField(name: String): Boolean? =
	when (val value = fields[name]) {
		is Boolean -> value
		is String -> value.toBooleanStrictOrNull()
		else -> null
	}
