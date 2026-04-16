package io.github.lishangbu.avalon.shared.infra.mutiny

import io.smallrye.mutiny.Uni
import kotlinx.coroutines.future.await

/**
 * 将 Mutiny [Uni] 挂起等待为 Kotlin 协程结果。
 *
 * 这是一份跨模块共用的桥接实现，统一收口 `subscribeAsCompletionStage().await()` 的样板代码。
 *
 * @return `Uni` 成功完成后的结果值。
 */
suspend fun <T> Uni<T>.awaitSuspending(): T = subscribeAsCompletionStage().await()