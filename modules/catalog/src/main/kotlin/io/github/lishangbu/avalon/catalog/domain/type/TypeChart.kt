package io.github.lishangbu.avalon.catalog.domain.type

import io.github.lishangbu.avalon.catalog.domain.TypeDefinition
import io.github.lishangbu.avalon.catalog.domain.TypeEffectiveness

/**
 * Catalog 对外暴露的属性矩阵快照。
 *
 * @property types 当前属性定义列表。
 * @property entries 当前属性克制矩阵中的全部关系项。
 */
data class TypeChart(
    val types: List<TypeDefinition>,
    val entries: List<TypeEffectiveness>,
)
