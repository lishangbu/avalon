package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.TypeEffectivenessEntry
import io.github.lishangbu.avalon.dataset.entity.TypeEffectivenessEntryId
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/**
 * 属性相克条目仓储接口
 *
 * 定义属性相克矩阵单元格的查询与持久化操作。
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface TypeEffectivenessEntryRepository {
    /** 按条件查询属性相克条目列表 */
    fun findAll(
        attackingTypeId: Long?,
        defendingTypeId: Long?,
        multiplierPercent: Int?,
    ): List<TypeEffectivenessEntry>

    /** 按条件分页查询属性相克条目 */
    fun findPage(
        attackingTypeId: Long?,
        defendingTypeId: Long?,
        multiplierPercent: Int?,
        pageable: Pageable,
    ): Page<TypeEffectivenessEntry>

    /** 保存属性相克条目 */
    fun save(typeEffectivenessEntry: TypeEffectivenessEntry): TypeEffectivenessEntry

    /** 按 ID 删除属性相克条目 */
    fun deleteById(id: TypeEffectivenessEntryId)
}
