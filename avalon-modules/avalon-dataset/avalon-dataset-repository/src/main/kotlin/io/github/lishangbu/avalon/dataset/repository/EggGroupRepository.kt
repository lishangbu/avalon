package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.EggGroup
import io.github.lishangbu.avalon.dataset.entity.dto.EggGroupSpecification
import org.babyfish.jimmer.spring.repository.KRepository

/** 蛋组仓储接口 */
interface EggGroupRepository :
    KRepository<EggGroup, Long>,
    EggGroupRepositoryExt

/** 蛋组仓储扩展接口 */
interface EggGroupRepositoryExt {
    /** 按条件查询蛋组列表 */
    fun findAll(specification: EggGroupSpecification?): List<EggGroup>

    /** 按 ID 删除蛋组 */
    fun removeById(id: Long)
}
