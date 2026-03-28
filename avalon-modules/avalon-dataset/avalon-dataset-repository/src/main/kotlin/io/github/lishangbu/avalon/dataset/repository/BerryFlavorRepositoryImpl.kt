package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.BerryFlavor
import io.github.lishangbu.avalon.dataset.entity.dto.BerryFlavorSpecification
import io.github.lishangbu.avalon.dataset.entity.id
import io.github.lishangbu.avalon.jimmer.support.readOrNull
import org.babyfish.jimmer.sql.ast.mutation.SaveMode
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository

@Repository
class BerryFlavorRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : BerryFlavorRepository {
    /** 查询全部树果风味列表 */
    override fun findAll(): List<BerryFlavor> =
        sql
            .createQuery(BerryFlavor::class) {
                select(table)
            }.execute()

    /** 按条件查询树果风味列表 */
    override fun findAll(specification: BerryFlavorSpecification?): List<BerryFlavor> =
        sql
            .createQuery(BerryFlavor::class) {
                specification?.let { where(it) }
                select(table)
            }.execute()

    /** 按 ID 查询树果风味 */
    override fun findById(id: Long): BerryFlavor? = sql.findById(BerryFlavor::class, id)

    /** 保存树果风味 */
    override fun save(berryFlavor: BerryFlavor): BerryFlavor =
        sql
            .save(berryFlavor) {
                val mode = berryFlavor.readOrNull { id }?.let { SaveMode.UPSERT } ?: SaveMode.INSERT_ONLY
                setMode(mode)
            }.modifiedEntity

    /** 保存树果风味并立即刷新 */
    override fun saveAndFlush(berryFlavor: BerryFlavor): BerryFlavor = save(berryFlavor)

    /** 按 ID 删除树果风味 */
    override fun deleteById(id: Long) {
        sql
            .createDelete(BerryFlavor::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }

    /** 刷新持久化上下文 */
    override fun flush() = Unit
}
