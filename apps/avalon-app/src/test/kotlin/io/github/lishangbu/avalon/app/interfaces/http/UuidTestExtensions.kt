package io.github.lishangbu.avalon.app.interfaces.http

import java.util.UUID

/**
 * 把接口返回的主键值转换成 `UUID`，避免测试重复写样板解析代码。
 */
fun Any?.toUuid(): UUID = when (this) {
    is UUID -> this
    is String -> UUID.fromString(this)
    else -> UUID.fromString(requireNotNull(this).toString())
}
