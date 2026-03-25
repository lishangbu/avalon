package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import org.babyfish.jimmer.sql.kt.KSqlClient
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.babyfish.jimmer.sql.kt.ast.expression.ilike
import org.springframework.data.domain.Example
import org.springframework.stereotype.Repository

@Repository
class StatRepositoryImpl(
    /** Jimmer SQL 客户端 */
    private val sql: KSqlClient,
) : StatRepository {
    /** 按条件查询能力值列表 */
    override fun findAll(example: Example<Stat>?): List<Stat> {
        val probe = example?.probe
        return sql
            .createQuery(Stat::class) {
                probe.readOrNull { id }?.let { where(table.id eq it) }
                probe.readOrNull { name }.takeFilter()?.let { where(table.name ilike "%$it%") }
                probe.readOrNull { internalName }.takeFilter()?.let { where(table.internalName ilike "%$it%") }
                probe.readOrNull { gameIndex }?.let { where(table.gameIndex eq it) }
                probe.readOrNull { battleOnly }?.let { where(table.battleOnly eq it) }
                probe.readOrNull { moveDamageClassId }?.let { where(table.moveDamageClassId eq it) }
                select(table)
            }.execute()
    }

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
