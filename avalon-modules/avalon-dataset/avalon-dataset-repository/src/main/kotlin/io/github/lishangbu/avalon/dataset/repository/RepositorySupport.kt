package io.github.lishangbu.avalon.dataset.repository

import org.babyfish.jimmer.UnloadedException

/** 去除空白后返回非空字符串 */
internal fun String?.takeFilter(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

/** 安全读取未加载属性 */
internal inline fun <T, R> T?.readOrNull(block: T.() -> R): R? =
    try {
        this?.block()
    } catch (_: UnloadedException) {
        null
    }
