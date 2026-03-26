package io.github.lishangbu.avalon.dataset.service

import io.github.lishangbu.avalon.dataset.entity.dto.MoveDamageClassSpecification
import io.github.lishangbu.avalon.dataset.entity.dto.MoveDamageClassView
import io.github.lishangbu.avalon.dataset.entity.dto.SaveMoveDamageClassInput
import io.github.lishangbu.avalon.dataset.entity.dto.UpdateMoveDamageClassInput
import org.babyfish.jimmer.Page
import org.springframework.data.domain.Pageable

/** 招式伤害分类服务 */
interface MoveDamageClassService {
    /** 按条件分页查询招式伤害分类*/
    fun getPageByCondition(
        specification: MoveDamageClassSpecification,
        pageable: Pageable,
    ): Page<MoveDamageClassView>

    /** 保存招式伤害分类 */
    fun save(command: SaveMoveDamageClassInput): MoveDamageClassView

    /** 更新招式伤害分类 */
    fun update(command: UpdateMoveDamageClassInput): MoveDamageClassView

    /** 按 ID 删除招式伤害分类 */
    fun removeById(id: Long)

    /** 根据条件查询招式伤害分类列表 */
    fun listByCondition(specification: MoveDamageClassSpecification): List<MoveDamageClassView>
}
