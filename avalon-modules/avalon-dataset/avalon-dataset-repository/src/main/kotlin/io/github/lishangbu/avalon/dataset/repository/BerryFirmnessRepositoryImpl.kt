package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFirmnessSpecification
import io.github.lishangbu.avalon.dataset.entity.id
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class BerryFirmnessRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : BerryFirmnessRepositoryExt {
    /** 按条件查询树果硬度列表 */
    override fun findAll(specification: BerryFirmnessSpecification?): List<BerryFirmness> =
        sql
            .createQuery(BerryFirmness::class) {
                specification?.let { where(it) }
                select(table)
            }.execute()

    /** 按条件分页查询树果硬度 */
    override fun findAll(
        specification: BerryFirmnessSpecification?,
        pageable: Pageable,
    ): Page<BerryFirmness> =
        sql
            .createQuery(BerryFirmness::class) {
                specification?.let { where(it) }
                select(table)
            }.fetchPage(pageable.pageNumber, pageable.pageSize)

    /** 按 ID 删除树果硬度 */
    override fun removeById(id: Long) {
        sql
            .createDelete(BerryFirmness::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }
}
