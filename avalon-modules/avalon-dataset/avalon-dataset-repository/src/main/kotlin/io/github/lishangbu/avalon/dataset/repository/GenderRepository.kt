package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Gender
import io.github.lishangbu.avalon.dataset.entity.dto.GenderSpecification
import org.babyfish.jimmer.spring.repository.KRepository

/**
 * 性别仓储接口
 *
 * 定义性别数据的查询与持久化操作
 *
 * @author lishangbu
 * @since 2026/3/24
 */
interface GenderRepository :
    KRepository<Gender, Long>,
    GenderRepositoryExt

/** 性别仓储扩展接口 */
interface GenderRepositoryExt {
    /** 按条件查询性别列表 */
    fun findAll(specification: GenderSpecification?): List<Gender>

    /** 按 ID 删除性别 */
    fun removeById(id: Long)
}
