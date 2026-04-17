package io.github.lishangbu.avalon.catalog.interfaces.http.type

import io.github.lishangbu.avalon.catalog.domain.TypeEffectivenessDraft
import jakarta.validation.Valid
import jakarta.validation.constraints.NotEmpty

/**
 * 整表替换属性矩阵时使用的请求体。
 *
 * @property entries 新的完整矩阵条目集合。
 */
data class UpsertTypeChartRequest(
    @field:NotEmpty
    val entries: List<@Valid UpsertTypeChartEntryRequest>,
)

/**
 * 将矩阵写入请求转换为领域草稿列表。
 */
fun UpsertTypeChartRequest.toDrafts(): List<TypeEffectivenessDraft> = entries.map { it.toDraft() }
