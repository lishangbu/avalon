package io.github.lishangbu.avalon.catalog.domain.type

import io.github.lishangbu.avalon.catalog.domain.TypeEffectiveness
import io.github.lishangbu.avalon.catalog.domain.TypeEffectivenessDraft

/**
 * 属性克制矩阵仓储契约。
 *
 * 该仓储面向整张 `type chart` 的读写，不再暴露单条属性克制关系的行级 CRUD。
 */
interface TypeChartRepository {
    /**
     * 列出当前已持久化的全部属性克制关系。
     */
    suspend fun listEntries(): List<TypeEffectiveness>

    /**
     * 用一整批属性克制关系替换当前矩阵。
     */
    suspend fun replaceEntries(entries: List<TypeEffectivenessDraft>)
}
