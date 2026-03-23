package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Type
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Example
import org.springframework.data.domain.Pageable

/**
 * 属性(Type)数据访问层
 *
 * 由 Jimmer 实现的仓储契约，保持原有调用方式兼容
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface TypeRepository {
    fun findAll(): List<Type>

    fun findAll(example: Example<Type>?): List<Type>

    fun findAll(
        example: Example<Type>?,
        pageable: Pageable,
    ): Page<Type>

    fun findById(id: Long): Type?

    fun save(type: Type): Type

    fun saveAndFlush(type: Type): Type

    fun deleteById(id: Long)

    fun flush()
}
