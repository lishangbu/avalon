package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.StatSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.StatView
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.stereotype.Repository

@Repository
class StatRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : StatRepository {
    /** 按条件查询能力值列表 */
    override fun findAll(specification: StatSpecification?): List<StatView> =
        sql
            .createQuery(Stat::class) {
                specification?.let { where(it) }
                select(table.fetch(StatView::class))
            }.execute()

    /** 按 ID 查询能力值 */
    override fun findById(id: Long): StatView? =
        sql
            .createQuery(Stat::class) {
                where(table.id eq id)
                select(table.fetch(StatView::class))
            }.execute()
            .firstOrNull()

    /** 保存能力值 */
    override fun save(stat: Stat): Stat = sql.save(stat).modifiedEntity

    /** 按 ID 删除能力值 */
    override fun deleteById(id: Long) {
        sql
            .createDelete(Stat::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }
}
