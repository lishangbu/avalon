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
) : StatRepositoryExt {
    /** 按条件查询能力值列表 */
    override fun findAll(specification: StatSpecification?): List<StatView> =
        sql
            .createQuery(Stat::class) {
                specification?.let { where(it) }
                select(table.fetch(StatView::class))
            }.execute()

    /** 按 ID 查询能力值 */
    override fun findViewById(id: Long): StatView? =
        sql
            .createQuery(Stat::class) {
                where(table.id eq id)
                select(table.fetch(StatView::class))
            }.execute()
            .firstOrNull()

    /** 按 ID 删除能力值 */
    override fun removeById(id: Long) {
        sql
            .createDelete(Stat::class) {
                where(table.id eq id)
                disableDissociation()
            }.execute()
    }
}
