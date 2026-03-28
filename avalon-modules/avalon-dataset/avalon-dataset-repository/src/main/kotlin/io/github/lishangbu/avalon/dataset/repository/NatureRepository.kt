package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Nature
import io.github.lishangbu.avalon.dataset.entity.dto.NatureSpecification
import org.babyfish.jimmer.spring.repository.KRepository

/** 性格仓储接口 */
interface NatureRepository :
    KRepository<Nature, Long>,
    NatureRepositoryExt

/** 性格仓储扩展接口 */
interface NatureRepositoryExt {
    /** 按条件查询性格列表 */
    fun findAll(specification: NatureSpecification?): List<Nature>

    /** 按 ID 查询性格及其关联 */
    fun findByIdWithAssociations(id: Long): Nature?

    /** 按 ID 删除性格 */
    fun removeById(id: Long)
}
