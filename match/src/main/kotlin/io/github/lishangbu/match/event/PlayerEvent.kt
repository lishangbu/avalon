package io.github.lishangbu.match.event

/** 通知只声明哪个权威 REST 资源已变化，不复制资源内容。 */
data class PlayerEvent(val type: String, val resourceId: String?, val revision: Long?)
