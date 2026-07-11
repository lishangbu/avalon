package io.github.lishangbu.battlesession.runtime

import java.util.UUID

/** 生成 Runtime 拥有且不复用的 Session Identifier。 */
internal fun interface SessionIdentifierGenerator {
	fun generate(): String

	/** 使用 UUID v4 生成可直接用于节点路由的不透明字符串标识。 */
	class Uuid : SessionIdentifierGenerator {
		override fun generate(): String = UUID.randomUUID().toString()
	}
}
