package io.github.lishangbu.avalon.jimmer.support

import org.babyfish.jimmer.UnloadedException

/** 安全读取可能未加载的 Jimmer 属性。 */
inline fun <T, R> T?.readOrNull(block: T.() -> R): R? =
    try {
        this?.block()
    } catch (_: UnloadedException) {
        null
    }
