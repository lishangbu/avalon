package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import org.babyfish.jimmer.Page
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

/**
 * 树果仓储实现
 *
 * 基于 Jimmer 查询并持久化树果数据
 */
@Repository
class BerryRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : BerryRepository {
    /** 按示例条件查询树果列表 */
    override fun findAll(example: Example<Berry>?): List<Berry> {
        val probe = example?.probe
        return sql
            .createQuery(Berry::class) {
                probe.readOrNull { id }?.let { where(table.id eq it) }
                probe.readOrNull { name }.takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readOrNull { internalName }.takeFilter()?.let { where(table.internalName ilike "%$it%") }
                probe.readOrNull { growthTime }?.let { where(table.growthTime eq it) }
                probe.readOrNull { maxHarvest }?.let { where(table.maxHarvest eq it) }
                probe.readOrNull { bulk }?.let { where(table.bulk eq it) }
                probe.readOrNull { smoothness }?.let { where(table.smoothness eq it) }
                probe.readOrNull { soilDryness }?.let { where(table.soilDryness eq it) }
                probe.readOrNull { naturalGiftPower }?.let { where(table.naturalGiftPower eq it) }
                probe.readOrNull { berryFirmnessId }?.let { where(table.berryFirmnessId eq it) }
                probe.readOrNull { naturalGiftTypeId }?.let { where(table.naturalGiftTypeId eq it) }
                select(table)
            }.execute()
    }

    /** 按示例条件分页查询树果 */
    override fun findAll(
        example: Example<Berry>?,
        pageable: Pageable,
    ): Page<Berry> {
        val probe = example?.probe
        return sql
            .createQuery(Berry::class) {
                probe.readOrNull { id }?.let { where(table.id eq it) }
                probe.readOrNull { name }.takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readOrNull { internalName }.takeFilter()?.let { where(table.internalName ilike "%$it%") }
                probe.readOrNull { growthTime }?.let { where(table.growthTime eq it) }
                probe.readOrNull { maxHarvest }?.let { where(table.maxHarvest eq it) }
                probe.readOrNull { bulk }?.let { where(table.bulk eq it) }
                probe.readOrNull { smoothness }?.let { where(table.smoothness eq it) }
                probe.readOrNull { soilDryness }?.let { where(table.soilDryness eq it) }
                probe.readOrNull { naturalGiftPower }?.let { where(table.naturalGiftPower eq it) }
                probe.readOrNull { berryFirmnessId }?.let { where(table.berryFirmnessId eq it) }
                probe.readOrNull { naturalGiftTypeId }?.let { where(table.naturalGiftTypeId eq it) }
                select(table)
            }.fetchPage(pageable.pageNumber, pageable.pageSize)
    }

    /** 按 ID 查询单个树果 */
    override fun findById(id: Long): Berry? = sql.findById(Berry::class, id)

    /** 保存树果 */
    override fun save(berry: Berry): Berry = sql.save(berry).modifiedEntity

    /** 保存树果并立即刷新 */
    override fun saveAndFlush(berry: Berry): Berry = save(berry)

    /** 删除指定 ID 的树果 */
    override fun deleteById(id: Long) {
        sql
            .createDelete(Berry::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    /** 保留与 Spring Data 风格一致的刷新方法 */
    override fun flush() = Unit
}
