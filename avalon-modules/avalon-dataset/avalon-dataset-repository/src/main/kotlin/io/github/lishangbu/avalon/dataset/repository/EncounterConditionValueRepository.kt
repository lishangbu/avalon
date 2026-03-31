package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionValueSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.EncounterConditionValueView
import org.babyfish.jimmer.spring.repository.KRepository
import org.babyfish.jimmer.spring.repository.orderBy
import org.babyfish.jimmer.sql.kt.ast.expression.eq
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository

/** 遭遇条件值仓储接口 */
@Repository
interface EncounterConditionValueRepository : KRepository<EncounterConditionValue, Long> {
    /** 按条件查询遭遇条件值视图 */
    fun listViews(specification: EncounterConditionValueSpecification?): List<EncounterConditionValueView> =
        sql
            .createQuery(EncounterConditionValue::class) {
                specification?.let(::where)
                orderBy(DEFAULT_SORT)
                select(table.fetch(EncounterConditionValueView::class))
            }.execute()

    /** 按 ID 查询遭遇条件值视图 */
    fun loadViewById(id: Long): EncounterConditionValueView? =
        sql
            .createQuery(EncounterConditionValue::class) {
                where(table.id eq id)
                orderBy(DEFAULT_SORT)
                select(table.fetch(EncounterConditionValueView::class))
            }.execute()
            .firstOrNull()

    /** 判断指定遭遇条件下是否存在遭遇条件值 */
    fun existsByEncounterConditionId(encounterConditionId: Long): Boolean =
        sql
            .createQuery(EncounterConditionValue::class) {
                where(table.encounterCondition.id eq encounterConditionId)
                orderBy(DEFAULT_SORT)
                select(table.id)
            }.execute()
            .isNotEmpty()

    companion object {
        private val DEFAULT_SORT: Sort = Sort.by(Sort.Order.asc("id"))
    }
}
