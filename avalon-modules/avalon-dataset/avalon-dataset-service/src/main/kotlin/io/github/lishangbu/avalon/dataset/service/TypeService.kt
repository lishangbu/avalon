package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.Type
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/** 属性服务。 */
interface TypeService {
    /** 根据条件分页查询属性。 */
    fun getPageByCondition(
        type: Type,
        pageable: Pageable,
    ): Page<Type>

    /** 新增属性。 */
    fun save(type: Type): Type

    /** 更新属性。 */
    fun update(type: Type): Type

    /** 根据主键删除属性。 */
    fun removeById(id: Long)

    /** 根据条件查询属性列表。 */
    fun listByCondition(type: Type): List<Type>
}
