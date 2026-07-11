package io.github.lishangbu.battlesession

import java.util.UUID

fun interface SessionIdentifierGenerator {
	fun generate(): String
}

internal class UuidSessionIdentifierGenerator : SessionIdentifierGenerator {
	override fun generate(): String = UUID.randomUUID().toString()
}
