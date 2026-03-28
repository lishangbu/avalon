package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Type
import io.github.lishangbu.avalon.dataset.entity.dto.TypeSpecification
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 属性仓储接口
 *
 * 定义属性数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2025/09/14
 */
interface TypeRepository :
    KRepository<Type, Long>,
    TypeRepositoryExt

/** 属性仓储扩展接口 */
interface TypeRepositoryExt {
    /** 按条件查询属性列表 */
    fun findAll(specification: TypeSpecification?): List<Type>

    /** 按 ID 删除属性 */
    fun removeById(id: Long)
}
