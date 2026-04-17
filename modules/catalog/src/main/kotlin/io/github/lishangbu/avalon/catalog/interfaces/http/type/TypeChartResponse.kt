package io.github.lishangbu.avalon.catalog.interfaces.http.type

import io.github.lishangbu.avalon.catalog.domain.type.TypeChart

/**
 * 属性矩阵响应。
 *
 * @property types 当前矩阵涉及的全部属性定义。
 * @property entries 当前矩阵中的全部克制关系项。
 */
data class TypeChartResponse(
    val types: List<TypeDefinitionResponse>,
    val entries: List<TypeChartEntryResponse>,
)

/**
 * 将属性矩阵快照转换为 HTTP 响应。
 */
fun TypeChart.toResponse(): TypeChartResponse =
    TypeChartResponse(
        types = types.map { it.toResponse() },
        entries = entries.map { it.toChartEntryResponse() },
    )
