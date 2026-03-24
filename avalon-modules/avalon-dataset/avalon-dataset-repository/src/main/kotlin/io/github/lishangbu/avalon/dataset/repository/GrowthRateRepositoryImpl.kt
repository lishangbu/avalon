package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.GrowthRate
import io.github.lishangbu.avalon.dataset.entity.description
import io.github.lishangbu.avalon.dataset.entity.id
import io.github.lishangbu.avalon.dataset.entity.internalName
import io.github.lishangbu.avalon.dataset.entity.name
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.springframework.data.domain.Example
import org.springframework.stereotype.Repository

@Repository
class GrowthRateRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : GrowthRateRepository {
    /** 查询全部成长速率列表 */
    override fun findAll(): List<GrowthRate> =
        sql
            .createQuery(GrowthRate::class) {
                select(table)
            }.execute()

    /** 按条件查询成长速率列表 */
    override fun findAll(example: Example<GrowthRate>?): List<GrowthRate> {
        val probe = example?.probe
        return sql
            .createQuery(GrowthRate::class) {
                probe.readOrNull { id }?.let { where(table.id eq it) }
                probe.readOrNull { name }.takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readOrNull { internalName }.takeFilter()?.let { where(table.internalName ilike "%$it%") }
                probe.readOrNull { description }.takeFilter()?.let { where(table.description ilike "%$it%") }
                select(table)
            }.execute()
    }

    /** 按 ID 查询成长速率 */
    override fun findById(id: Long): GrowthRate? = sql.findById(GrowthRate::class, id)

    /** 保存成长速率 */
    override fun save(growthRate: GrowthRate): GrowthRate =
        sql
            .save(growthRate) {
                val mode = growthRate.readOrNull { id }?.let { SaveMode.UPSERT } ?: SaveMode.INSERT_ONLY
                setMode(mode)
            }.modifiedEntity

    /** 保存成长速率并立即刷新 */
    override fun saveAndFlush(growthRate: GrowthRate): GrowthRate = save(growthRate)

    /** 按 ID 删除成长速率 */
    override fun deleteById(id: Long) {
        sql
            .createDelete(GrowthRate::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    /** 刷新持久化上下文 */
    override fun flush() = Unit
}
