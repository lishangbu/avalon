package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/** 招式伤害类别服务。 */
interface MoveDamageClassService {
    /** 根据条件分页查询招式伤害类别。 */
    fun getPageByCondition(
        moveDamageClass: MoveDamageClass,
        pageable: Pageable,
    ): Page<MoveDamageClass>

    /** 新增招式伤害类别。 */
    fun save(moveDamageClass: MoveDamageClass): MoveDamageClass

    /** 更新招式伤害类别。 */
    fun update(moveDamageClass: MoveDamageClass): MoveDamageClass

    /** 根据主键删除招式伤害类别。 */
    fun removeById(id: Long)

    /** 根据条件查询招式伤害类别列表。 */
    fun listByCondition(moveDamageClass: MoveDamageClass): List<MoveDamageClass>
}
