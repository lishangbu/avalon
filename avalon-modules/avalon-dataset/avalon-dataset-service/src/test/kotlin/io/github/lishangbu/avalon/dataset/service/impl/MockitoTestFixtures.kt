package io.github.lishangbu.avalon.dataset.service.impl

import org.mockito.Mockito

internal inline fun <reified T> any(): T = Mockito.any(T::class.java)

@Suppress("UNCHECKED_CAST")
internal fun <T> eq(value: T): T = Mockito.eq(value) ?: value

@Suppress("UNCHECKED_CAST")
internal fun <T> isNull(): T? = Mockito.isNull<T>()
