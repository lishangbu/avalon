package io.github.lishangbu.avalon.dataset.service.impl

import org.mockito.Mockito

internal inline fun <reified T> any(): T = Mockito.any(T::class.java)
