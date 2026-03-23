package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.MoveDamageClass
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/** 招式伤害分类服务 */
interface MoveDamageClassService {
    /** 按条件分页查询招式伤害分类*/
    fun getPageByCondition(
        moveDamageClass: MoveDamageClass,
        pageable: Pageable,
    ): Page<MoveDamageClass>

    /** 保存招式伤害分类 */
    fun save(moveDamageClass: MoveDamageClass): MoveDamageClass

    /** 更新招式伤害分类 */
    fun update(moveDamageClass: MoveDamageClass): MoveDamageClass

    /** 按 ID 删除招式伤害分类 */
    fun removeById(id: Long)

    /** 根据条件查询招式伤害分类列表 */
    fun listByCondition(moveDamageClass: MoveDamageClass): List<MoveDamageClass>
}
