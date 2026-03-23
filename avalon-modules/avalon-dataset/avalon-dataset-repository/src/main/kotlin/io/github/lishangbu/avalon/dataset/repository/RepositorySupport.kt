package io.github.lishangbu.avalon.dataset.repository

import org.babyfish.jimmer.UnloadedException

internal fun String?.takeFilter(): String? = this?.trim()?.takeIf { it.isNotEmpty() }

internal inline fun <T, R> T?.readOrNull(block: T.() -> R): R? =
    try {
        this?.block()
    } catch (_: UnloadedException) {
        null
    }
