package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.Type
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/** 属性应用服务 */
interface TypeService {
    /** 按筛选条件分页查询属性 */
    fun getPageByCondition(
        type: Type,
        pageable: Pageable,
    ): Page<Type>

    /** 创建属性 */
    fun save(type: Type): Type

    /** 更新属性 */
    fun update(type: Type): Type

    /** 删除指定 ID 的属性 */
    fun removeById(id: Long)

    /** 按筛选条件查询属性列表 */
    fun listByCondition(type: Type): List<Type>
}
