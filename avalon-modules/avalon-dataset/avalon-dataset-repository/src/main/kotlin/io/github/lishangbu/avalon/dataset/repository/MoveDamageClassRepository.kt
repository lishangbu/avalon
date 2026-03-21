package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable

/**
 * 招式伤害类别(MoveDamageClass)数据访问层
 *
 * 由 Jimmer 实现的仓储契约，保持原有调用方式兼容
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface MoveDamageClassRepository {
    fun findAll(example: Example<MoveDamageClass>?): List<MoveDamageClass>

    fun findAll(
        example: Example<MoveDamageClass>?,
        pageable: Pageable,
    ): Page<MoveDamageClass>

    fun save(moveDamageClass: MoveDamageClass): MoveDamageClass

    fun deleteById(id: Long)
}
