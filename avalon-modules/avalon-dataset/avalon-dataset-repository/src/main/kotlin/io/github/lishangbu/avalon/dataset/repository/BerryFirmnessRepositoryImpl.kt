package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.BerryFirmness
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFirmnessSpecification
import io.github.lishangbu.avalon.dataset.entity.id
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class BerryFirmnessRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : BerryFirmnessRepository {
    /** 查询全部树果硬度列表 */
    override fun findAll(): List<BerryFirmness> =
        sql
            .createQuery(BerryFirmness::class) {
                select(table)
            }.execute()

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

    /** 按 ID 查询树果硬度 */
    override fun findById(id: Long): BerryFirmness? = sql.findById(BerryFirmness::class, id)

    /** 保存树果硬度 */
    override fun save(berryFirmness: BerryFirmness): BerryFirmness =
        sql
            .save(berryFirmness) {
                val mode = berryFirmness.readOrNull { id }?.let { SaveMode.UPSERT } ?: SaveMode.INSERT_ONLY
                setMode(mode)
            }.modifiedEntity

    /** 保存树果硬度并立即刷新 */
    override fun saveAndFlush(berryFirmness: BerryFirmness): BerryFirmness = save(berryFirmness)

    /** 按 ID 删除树果硬度 */
    override fun deleteById(id: Long) {
        sql
            .createDelete(BerryFirmness::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    /** 刷新持久化上下文 */
    override fun flush() = Unit
}
