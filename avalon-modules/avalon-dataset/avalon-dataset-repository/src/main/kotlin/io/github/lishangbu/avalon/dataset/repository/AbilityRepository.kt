package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.Ability
import io.github.lishangbu.avalon.dataset.entity.dto.AbilitySpecification
import org.babyfish.jimmer.spring.repository.KRepository

/** 特性仓储接口 */
interface AbilityRepository :
    KRepository<Ability, Long>,
    AbilityRepositoryExt

/** 特性仓储扩展接口 */
interface AbilityRepositoryExt {
    /** 按条件查询特性列表 */
    fun findAll(specification: AbilitySpecification?): List<Ability>

    /** 按 ID 删除特性 */
    fun removeById(id: Long)
}
