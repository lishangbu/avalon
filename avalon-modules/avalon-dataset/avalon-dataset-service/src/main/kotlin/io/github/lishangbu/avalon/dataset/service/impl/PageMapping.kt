package io.github.lishangbu.avalon.dataset.service.impl

import org.babyfish.jimmer.Page

internal fun <S, T> Page<S>.mapRows(
    transform: (S) -> T,
): Page<T> = Page(rows.map(transform), totalRowCount, totalPageCount)
